package com.sight.repository

import com.sight.domain.groupmatching.GroupMatchingField
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface GroupMatchingFieldRepository : JpaRepository<GroupMatchingField, String> {
    fun existsByName(name: String): Boolean

<<<<<<< Updated upstream
    fun existsByNameAndObsoletedAtIsNull(name: String): Boolean

    fun findByIdAndObsoletedAtIsNull(id: String): Optional<GroupMatchingField>
=======
    fun findByName(name: String): GroupMatchingField?
>>>>>>> Stashed changes
}
