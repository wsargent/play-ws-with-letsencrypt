import scala.concurrent.ExecutionContext

package object contexts {
  class WSExecutionContext(val underlying: ExecutionContext) extends AnyVal
  class DefaultExecutionContext(val underlying: ExecutionContext) extends AnyVal

  implicit def wsToEC(implicit ec: WSExecutionContext) = ec.underlying
  implicit def defaultToEC(implicit ec: DefaultExecutionContext) = ec.underlying
}