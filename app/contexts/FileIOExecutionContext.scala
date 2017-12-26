package contexts

import scala.concurrent.ExecutionContext

class FileIOExecutionContext(val underlying: ExecutionContext) extends AnyVal
