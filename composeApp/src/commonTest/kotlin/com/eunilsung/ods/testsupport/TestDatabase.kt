package com.eunilsung.ods.testsupport

import com.eunilsung.ods.db.AppDatabase

/** 테스트마다 새로 만드는 빈 기기 DB. 플랫폼마다 인메모리 드라이버가 다르다. */
expect fun createTestDatabase(): AppDatabase
