package de.binarynoise.appdate

/**
 * Annotation for methods that do network operations
 * and that must not be called from the android main thread
 */
@Target(
	AnnotationTarget.FUNCTION,
	AnnotationTarget.PROPERTY_GETTER,
	AnnotationTarget.PROPERTY_SETTER,
	AnnotationTarget.CONSTRUCTOR
)
@Retention(AnnotationRetention.SOURCE)
annotation class RunInBackground
