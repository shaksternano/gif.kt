package com.shakster.gifkt

import kotlinx.io.files.Path

expect fun RandomAccessData.Companion.of(path: Path): RandomAccessData

expect fun Path.asRandomAccess(): RandomAccessData
