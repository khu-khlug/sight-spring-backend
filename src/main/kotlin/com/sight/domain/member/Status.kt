enum class StudentStatus(val code: Int) {
    UNITED(-1),
    ABSENCE(0),
    UNDERGRADUATE(1),
    GRADUATE(2),
}

enum class UserStatus(val code: Long) {
    INACTIVE(-1),
    UNAUTHORIZED(0),
    ACTIVE(1),
}
