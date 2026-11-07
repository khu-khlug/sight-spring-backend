package com.sight.core.groupauth

import com.lemonappdev.konsist.api.Konsist
import com.sight.core.auth.Auth
import com.sight.core.auth.UserRole
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.util.ReflectionUtils
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import kotlin.test.assertTrue

class GroupAuthDeclarationTest {
    @Test
    fun `그룹 권한 선언은 Auth 다음에 이름 있는 인자로 작성한다`() {
        val violations = mutableListOf<String>()
        Konsist.scopeFromDirectory("src/main/kotlin").functions().forEach { function ->
            val groupAuth =
                function.annotations.singleOrNull { it.fullyQualifiedName == GroupAuth::class.qualifiedName }
                    ?: return@forEach
            if (function.containingFile.packagee?.name?.startsWith("com.sight.controllers.http") != true) {
                violations += "${function.location}: GroupAuth는 HTTP Controller에만 선언해야 합니다."
            }
            val auth = function.annotations.singleOrNull { it.fullyQualifiedName == "com.sight.core.auth.Auth" }
            if (auth == null || function.annotations.indexOf(auth) > function.annotations.indexOf(groupAuth)) {
                violations += "${function.location}: Auth는 GroupAuth 위에 직접 선언해야 합니다."
            }
            if (groupAuth.arguments.any { !Regex("^\\s*(roles|groupId|recordId|allowManager)\\s*=").containsMatchIn(it.text) }) {
                violations += "${function.location}: GroupAuth에는 이름 있는 인자만 사용해야 합니다."
            }
        }
        assertTrue(violations.isEmpty(), violations.joinToString("\n"))
    }

    @Test
    fun `그룹 권한의 역할과 경로 변수 선언을 검사한다`() {
        val scanner = ClassPathScanningCandidateComponentProvider(false)
        scanner.addIncludeFilter(AnnotationTypeFilter(RestController::class.java))
        scanner.findCandidateComponents("com.sight.controllers.http").forEach { bean ->
            ReflectionUtils.getAllDeclaredMethods(Class.forName(requireNotNull(bean.beanClassName))).forEach { method ->
                method.getDeclaredAnnotation(GroupAuth::class.java)?.let { GroupAuthDeclaration.requireValid(method, it) }
            }
        }
    }

    @Test
    fun `Auth 누락과 잘못된 역할 및 경로 선언은 거부한다`() {
        InvalidDeclarations::class.java.declaredMethods.forEach { method ->
            val declaration = method.getDeclaredAnnotation(GroupAuth::class.java) ?: return@forEach
            assertThrows<AssertionError> { GroupAuthDeclaration.requireValid(method, declaration) }
        }
    }

    class InvalidDeclarations {
        @GroupAuth(roles = [GroupRole.MEMBER], groupId = "groupId", allowManager = false)
        @GetMapping("/groups/{groupId}")
        fun missingAuth(
            @PathVariable groupId: Long,
        ): Long = groupId

        @Auth([UserRole.USER])
        @GroupAuth(roles = [GroupRole.MEMBER], groupId = "groupId", allowManager = false)
        @GetMapping("/groups/{groupId}")
        fun wrongUserRoles(
            @PathVariable groupId: Long,
        ): Long = groupId

        @Auth([UserRole.USER, UserRole.MANAGER])
        @GroupAuth(roles = [GroupRole.WRITER], groupId = "groupId", allowManager = false)
        @GetMapping("/groups/{groupId}")
        fun missingRecord(
            @PathVariable groupId: Long,
        ): Long = groupId

        @Auth([UserRole.USER, UserRole.MANAGER])
        @GroupAuth(roles = [GroupRole.MEMBER], groupId = "", allowManager = false)
        @GetMapping("/groups/{groupId}")
        fun blankGroup(
            @PathVariable groupId: Long,
        ): Long = groupId

        @Auth([UserRole.USER, UserRole.MANAGER])
        @GroupAuth(roles = [GroupRole.MEMBER], groupId = "targetGroupId", allowManager = false)
        @GetMapping("/groups/{groupId}")
        fun wrongPathVariable(
            @PathVariable groupId: Long,
        ): Long = groupId

        @Auth([UserRole.USER, UserRole.MANAGER])
        @GroupAuth(roles = [GroupRole.MEMBER], groupId = "groupId", allowManager = false)
        @GetMapping("/groups/{groupId}")
        fun missingParameter(): Unit = Unit

        @Auth([UserRole.USER, UserRole.MANAGER])
        @GroupAuth(roles = [GroupRole.MEMBER], groupId = "groupId", allowManager = false)
        fun missingMapping(
            @PathVariable groupId: Long,
        ): Long = groupId
    }
}
