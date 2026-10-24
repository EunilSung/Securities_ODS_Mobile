package com.eunilsung.ods.util

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** 기기에서 새로 만드는 기록의 id. */
@OptIn(ExperimentalUuidApi::class)
fun newId(): String = Uuid.random().toString()
