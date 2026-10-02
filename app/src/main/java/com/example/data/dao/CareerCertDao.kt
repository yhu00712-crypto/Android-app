package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CareerCertDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOccupations(occupations: List<Occupation>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCertificates(certificates: List<Certificate>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelations(relations: List<OccupationCertificateCrossRef>)

    @Query("""
        SELECT c.*, r.requirementType, r.benefitDescription 
        FROM certificates c
        INNER JOIN occupation_certificate_cross_ref r ON c.id = r.certificateId
        WHERE r.occupationId = :occupationId
        ORDER BY CASE WHEN r.requirementType = 'MANDATORY' THEN 1 ELSE 2 END, c.difficulty DESC
    """)
    fun getCertificatesForOccupation(occupationId: String): Flow<List<CertificateWithRelationInfo>>

    @Query("""
        SELECT o.*, r.requirementType, r.benefitDescription 
        FROM occupations o
        INNER JOIN occupation_certificate_cross_ref r ON o.id = r.occupationId
        WHERE r.certificateId = :certificateId
        ORDER BY CASE WHEN r.requirementType = 'MANDATORY' THEN 1 ELSE 2 END, o.name ASC
    """)
    fun getOccupationsForCertificate(certificateId: String): Flow<List<OccupationWithRelationInfo>>

    @Query("""
        SELECT * FROM occupations 
        WHERE name LIKE '%' || :query || '%' 
           OR category LIKE '%' || :query || '%' 
           OR description LIKE '%' || :query || '%'
        ORDER BY 
           CASE WHEN name LIKE '%' || :query || '%' THEN 1 ELSE 2 END,
           name ASC
    """)
    fun searchOccupations(query: String): Flow<List<Occupation>>

    @Query("""
        SELECT * FROM certificates 
        WHERE name LIKE '%' || :query || '%' 
           OR authority LIKE '%' || :query || '%'
           OR description LIKE '%' || :query || '%'
        ORDER BY 
           CASE WHEN name LIKE '%' || :query || '%' THEN 1 ELSE 2 END,
           name ASC
    """)
    fun searchCertificates(query: String): Flow<List<Certificate>>

    @Query("SELECT * FROM occupations WHERE category = :category ORDER BY name ASC")
    fun getOccupationsByCategory(category: String): Flow<List<Occupation>>

    @Query("SELECT COUNT(*) FROM occupations")
    suspend fun getOccupationsCount(): Int

    @Query("DELETE FROM occupation_certificate_cross_ref")
    suspend fun clearRelations()

    @Query("DELETE FROM occupations")
    suspend fun clearOccupations()

    @Query("DELETE FROM certificates")
    suspend fun clearCertificates()
}
