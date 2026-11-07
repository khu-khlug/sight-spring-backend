package com.sight.core.groupauth

import com.sight.core.auth.Auth
import com.sight.core.auth.UserRole
import org.springframework.core.DefaultParameterNameDiscoverer
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import java.lang.reflect.Method

object GroupAuthDeclaration {
    private val parameterNames = DefaultParameterNameDiscoverer()

    fun requireValid(
        method: Method,
        groupAuth: GroupAuth,
    ) {
        fun requireDeclaration(
            condition: Boolean,
            message: String,
        ) {
            if (!condition) throw AssertionError("${method.declaringClass.simpleName}.${method.name}: $message")
        }

        val auth = method.getDeclaredAnnotation(Auth::class.java)
        requireDeclaration(
            auth != null && auth.roles.size == 2 && auth.roles.toSet() == setOf(UserRole.USER, UserRole.MANAGER),
            "GroupAuth에는 Auth의 USER·MANAGER 역할을 직접 함께 선언해야 합니다.",
        )
        requireDeclaration(groupAuth.roles.isNotEmpty(), "그룹 권한 배열은 비어 있을 수 없습니다.")
        requireDeclaration(groupAuth.groupId.isNotBlank(), "그룹 경로 변수 이름이 필요합니다.")
        requireDeclaration(
            GroupRole.WRITER !in groupAuth.roles || groupAuth.recordId.isNotBlank(),
            "작성자 권한에는 기록 경로 변수가 필요합니다.",
        )
        requireDeclaration(groupAuth.recordId.isEmpty() || groupAuth.recordId.isNotBlank(), "기록 경로 변수 이름은 공백일 수 없습니다.")

        val mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping::class.java)
        requireDeclaration(mapping != null, "GroupAuth는 HTTP 요청 처리 함수에만 선언할 수 있습니다.")
        val paths = mapping!!.path.toList() + mapping.value.toList()
        val names = parameterNames.getParameterNames(method)
        for (name in listOf(groupAuth.groupId, groupAuth.recordId).filter { it.isNotEmpty() }) {
            val variablePattern = Regex("\\{" + Regex.escape(name) + "(?::[^}]*)?\\}")
            requireDeclaration(
                paths.isNotEmpty() && paths.all { variablePattern.containsMatchIn(it) },
                "경로에 $name 변수가 없습니다.",
            )
            requireDeclaration(
                method.parameters.withIndex().any { (index, parameter) ->
                    val variable = parameter.getAnnotation(PathVariable::class.java)
                    variable != null &&
                        variable.name.ifEmpty { variable.value }.ifEmpty { names?.get(index).orEmpty() } == name
                },
                "$name 변수에 대응하는 PathVariable 파라미터가 없습니다.",
            )
        }
    }
}
