package de.binarynoise.appdate

import kotlin.annotation.AnnotationTarget.*

/**
 * Annotation for methods that do network operations
 * and that must not be called from the android main thread
 */
@Target(FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, CONSTRUCTOR)
@Retention(AnnotationRetention.SOURCE)
annotation class RunInBackground
