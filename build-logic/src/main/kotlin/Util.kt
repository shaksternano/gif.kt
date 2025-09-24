package com.shakster.gifkt.gradle

import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

fun KotlinSourceSet.registerChildSourceSets(vararg sourceSets: Any) {
    sourceSets.forEach {
        when (it) {
            is KotlinSourceSet -> it.dependsOn(this)

            is Provider<*> -> {
                when (val sourceSet = it.get()) {
                    is KotlinSourceSet -> sourceSet.dependsOn(this)

                    else -> throw IllegalArgumentException(
                        "Expected Provider<KotlinSourceSet>, got: Provider<${sourceSet::class.qualifiedName ?: "Any"}>"
                    )
                }
            }

            else -> throw IllegalArgumentException(
                "Expected KotlinSourceSet or Provider<KotlinSourceSet>, got: ${it::class.qualifiedName ?: "Any"}"
            )
        }
    }
}
