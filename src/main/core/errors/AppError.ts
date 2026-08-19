export class AppError extends Error {
  public readonly code: string
  public readonly isOperational: boolean

  constructor(message: string, code = 'INTERNAL_ERROR', isOperational = true) {
    super(message)
    this.name = this.constructor.name
    this.code = code
    this.isOperational = isOperational
    Error.captureStackTrace(this, this.constructor)
  }
}

export class DownloadError extends AppError {
  constructor(message: string, code = 'DOWNLOAD_FAILED') {
    super(message, code)
  }
}

export class TranscodeError extends AppError {
  constructor(message: string, code = 'TRANSCODE_FAILED') {
    super(message, code)
  }
}

export class MetadataError extends AppError {
  constructor(message: string, code = 'TAGGING_FAILED') {
    super(message, code)
  }
}

export class DependencyError extends AppError {
  constructor(message: string, code = 'DEPENDENCY_MISSING') {
    super(message, code)
  }
}
