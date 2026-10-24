package com.eunilsung.ods.data.local

import com.eunilsung.ods.db.AppDatabase
import com.eunilsung.ods.domain.repository.LocalDataWiper
import com.eunilsung.ods.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 기기 DB 의 업무 데이터와 시세 캐시를 모두 지우고, 그 데이터를 잠근 키도 버린다. 시세는 다음 로그인 때 다시 받는다.
 *
 * 지운 행이 DB 파일 안에 조각으로 남아도 잠긴 값이고 키가 없으니 풀 수 없다([LocalKeyGuard.shred]).
 * 업무 테이블을 새로 만들면 `AppDatabase.sq` 의 `wipeBusinessData` 에도 더한다.
 */
class LocalDataWiperImpl(
    private val database: AppDatabase,
    private val keyGuard: LocalKeyGuard,
) : LocalDataWiper {
    override suspend fun wipe() = withContext(Dispatchers.Default) {
        database.appDatabaseQueries.wipeBusinessData()
        keyGuard.shred()
        Log.message("[Wipe] business data wiped")
    }
}
