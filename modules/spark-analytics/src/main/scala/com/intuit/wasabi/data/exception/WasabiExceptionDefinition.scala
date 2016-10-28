package com.intuit.wasabi.data.exception

abstract class WasabiException(msg: String, ex: Throwable) extends Exception(msg, ex) {
  override def toString: String = s"${this.getClass.getName}[msg=$msg, ex=${ex.getMessage} ${ex.getStackTraceString}]"
}

class ConfigurationException(msg: String, ex: Throwable = null) extends WasabiException(msg, ex)
class ApplicationException(msg: String, ex: Throwable = null) extends WasabiException(msg, ex)
class ProcessorException(msg: String, ex: Throwable = null) extends WasabiException(msg, ex)
class RepositoryException(msg: String, ex: Throwable = null) extends WasabiException(msg, ex)

final case class UnderlineRepositoryException(msg: String, ex: Throwable) extends RepositoryException(msg, ex)
final case class UnderlineProcessorException(msg: String, ex: Throwable) extends RepositoryException(msg, ex)
final case class UnderlineApplicationException(msg: String, ex: Throwable) extends RepositoryException(msg, ex)

final case class InvalidInputApplicationException(msg: String) extends ApplicationException(msg)

final case class OperationNotSupportedException(msg: String) extends ProcessorException(msg)

final case class RepositoryWrappedProcessorException(msg: String, ex: RepositoryException) extends ProcessorException(msg, ex)
final case class ProcessorWrappedApplicationException(msg: String, ex: ProcessorException) extends ApplicationException(msg, ex)
final case class InvalidConfigurationError(msg: String) extends ConfigurationException(msg)

abstract class WasabiError (detail: WasabiException) {
  override def toString: String = s"${this.getClass.getName}[$detail]"
}
object WasabiError {
  final case class ConfigurationError(detail: ConfigurationException) extends WasabiError(detail)
  final case class ApplicationError(detail: ApplicationException) extends WasabiError(detail)
  final case class ProcessorError(detail: ProcessorException) extends WasabiError(detail)
  final case class RepositoryError(detail: RepositoryException) extends WasabiError(detail)
}
