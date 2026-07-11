package com.example.biblelog.domain.repository

import com.example.biblelog.domain.model.ChurchSummary
import com.example.biblelog.domain.model.SmallGroupSummary
import com.example.biblelog.domain.model.UserMemberships
import com.example.biblelog.domain.model.UserSearchResult

interface RelationRepository {
    suspend fun searchUsers(query: String): Result<List<UserSearchResult>>

    suspend fun searchChurches(query: String): Result<List<ChurchSummary>>

    suspend fun searchSmallGroups(query: String): Result<List<SmallGroupSummary>>

    suspend fun sendFriendRequest(userId: String): Result<Unit>

    suspend fun followUser(userId: String): Result<Unit>

    suspend fun joinChurch(churchId: String): Result<Unit>

    suspend fun joinSmallGroup(groupId: String): Result<Unit>

    suspend fun getFriendIds(): Result<Set<String>>

    suspend fun getFollowingIds(): Result<Set<String>>

    suspend fun getMemberships(): Result<UserMemberships>
}
