package com.sight.repository

import org.springframework.stereotype.Repository
import java.util.concurrent.Executor
import javax.sql.DataSource

@Repository
class FileMaintenanceLockRepository(private val dataSource: DataSource) {
    // MySQL named lock은 연결에 귀속되므로 작업이 끝날 때까지 같은 연결을 유지한다.
    // 외부 저장소 호출 중에는 트랜잭션과 행 잠금을 유지하지 않는다.
    fun withLock(
        name: String,
        action: () -> Unit,
    ): Boolean {
        dataSource.connection.use { connection ->
            val acquired =
                connection.prepareStatement("SELECT GET_LOCK(?, 0)").use { statement ->
                    statement.setString(1, name)
                    statement.executeQuery().use { result -> result.next() && result.getInt(1) == 1 }
                }
            if (!acquired) return false
            try {
                action()
            } finally {
                try {
                    connection.prepareStatement("SELECT RELEASE_LOCK(?)").use { statement ->
                        statement.setString(1, name)
                        statement.executeQuery().close()
                    }
                } catch (e: Exception) {
                    // 세션 잠금이 남은 물리 연결을 풀에 반환하지 않는다.
                    connection.abort(Executor { it.run() })
                    throw e
                }
            }
            return true
        }
    }
}
