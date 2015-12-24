###  Repository At: 
    https://github.com/snehamahesh/servicemodule
    
###  Design Notes:

    The services are assembled using modular structures. 
    The assembly of services is assumed to be hierarchical and should form a total order. 
    If a cyclic relationship were to be found in the dependency structure, it would make the service startup a cyclic deadlock. 
    
    The service configuration resembles a Directed Graph structure and can easily be constructed to find the dependency 
    order of the service startup with following characteristics:
    --- Every service may have a number of in-degree and an out-degree of dependent services.
    --- Startup of services can then be easily administered using one of synchronization mechanisms available, 
        using a set of CountDownLatch for in/out degree dependent services creates a barrier to start the service once the dependents have started.
        The leaf services, typically with no out-degree begins to start the service and cascades the dependents to unlock the startup process. 
    --- Since the synchronization is guarding the services to start at an appropriate invariant condition 
        allows all the services in the model can be started at the same time.
    --- Similarly stopping the services can be orchestrated using the out-degree associated with a given service.
    --- Start/Stopping an individual service is a partial use-case that can be supported easily there after.
    --- Data Structure is augmented to find the cycle within the service dependency configurations and aborts with ConfigCycleDetectedException.
    
    
    
###  Pending Actions:

    Comments and Code documentation, Logging integration (partially done). 
    
    
    