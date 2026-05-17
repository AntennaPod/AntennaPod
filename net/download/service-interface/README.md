# :net:download:service-interface

Interface of the download service. Enables other modules to call the download service without actually depending on the implementation.
`DownloadServiceInterface` is a singleton whose static implementation is registered in `:app` during `ClientConfigurator.initialize`.
