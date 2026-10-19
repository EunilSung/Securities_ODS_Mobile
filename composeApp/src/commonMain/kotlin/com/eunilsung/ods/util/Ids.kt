package com.eunilsung.ods.util

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** 기기에서 새로 만드는 기록의 id. 서버가 붙어도 같은 id 로 보내 한 번만 받게 한다. */
@OptIn(ExperimentalUuidApi::class)
fun newId(): String = Uuid.random().toString()
