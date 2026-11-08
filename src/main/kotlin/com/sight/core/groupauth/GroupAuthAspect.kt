package com.sight.core.groupauth

import com.sight.core.auth.AuthenticationHelper
import com.sight.core.auth.UserRole
import com.sight.core.exception.InternalServerErrorException
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.aop.support.AopUtils
import org.springframework.core.DefaultParameterNameDiscoverer
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.PathVariable

@Aspect
@Component
// 선언의 존재·내용·작성 순서는 린트가 검사하고, 실제 실행 순서는 @Order로 보장한다.
// AuthAspect(+100)의 인증·사용자 역할 검사가 끝난 뒤 그룹 권한을 검사한다.
@Order(Ordered.HIGHEST_PRECEDENCE + 101)
class GroupAuthAspect(
    private val groupAuthorizationService: GroupAuthorizationService,
) {
    private val parameterNames = DefaultParameterNameDiscoverer()

    @Around("@annotation(groupAuth)")
    fun checkGroupAuth(
        joinPoint: ProceedingJoinPoint,
        groupAuth: GroupAuth,
    ): Any? {
        val requester = AuthenticationHelper.requireCurrentRequester()
        val method =
            AopUtils.getMostSpecificMethod((joinPoint.signature as MethodSignature).method, joinPoint.target.javaClass)
        val names = parameterNames.getParameterNames(method)

        fun readId(name: String): Long {
            val index =
                method.parameters.withIndex().indexOfFirst { (index, parameter) ->
                    val variable = parameter.getAnnotation(PathVariable::class.java)
                    variable != null &&
                        variable.name.ifEmpty { variable.value }.ifEmpty { names?.get(index).orEmpty() } == name
                }
            return joinPoint.args.getOrNull(index) as? Long
                ?: throw InternalServerErrorException("경로 변수 $name 에 대응하는 Long 인자를 찾을 수 없습니다.")
        }

        groupAuthorizationService.requirePermission(
            groupId = readId(groupAuth.groupId),
            recordId = groupAuth.recordId.takeIf { it.isNotEmpty() }?.let(::readId),
            requesterId = requester.userId,
            isManager = requester.role == UserRole.MANAGER,
            permissions = groupAuth.permissions.toSet(),
            allowManager = groupAuth.allowManager,
        )
        return joinPoint.proceed()
    }
}
