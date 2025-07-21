package com.mhabulazm.samplegallery.domain


  class DomainMediaLoadException(
    message: String,
    cause: Throwable,
) : Exception(message, cause)