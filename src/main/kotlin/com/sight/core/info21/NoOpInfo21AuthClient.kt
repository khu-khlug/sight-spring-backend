package com.sight.core.info21

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("local")
class NoOpInfo21AuthClient : Info21AuthClient {
    override fun authenticate(request: Info21AuthRequest): StuauthResponse {
        return StuauthResponse(
            code = 200,
            message = "OK",
            data =
                StuauthData(
                    studentNumber = 2021999999,
                    name = "LOCAL_USER",
                    grade = 1,
                    major =
                        listOf(
                            StuauthMajor(
                                college = "소프트웨어융합대학",
                                department = "컴퓨터공학부",
                            ),
                        ),
                    phone = "010-1234-1234",
                ),
        )
    }
}
