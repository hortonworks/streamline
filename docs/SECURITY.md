# Running registry and streamline web-services securely

This module is intended to be used by registry and streamline web-services so that they can enable http client authentication via SPNEGO. The supported 
authentication mechanism is Kerberos. The code for this module has been borrowed from the hadoop-auth(2.7.3) module in Hadoop project and slightly modified. 
The reasons for doing so are to avoid having a dependency on hadoop-auth module which brings in some other modules, avoid conflicts with other versions of 
hadoop-auth module and having more control over the changes needed in future. Some text for this document has been borrowed from SECURITY.md of Apache Storm  

By default, registry and streamline web-services are running with authentication disabled and therefore anyone can access the web-services from ui/client as far
as they know the url and can access the web-server from the client machine. To enable client authentication, webservice needs to add a servlet filter from this
module. The webservice module will need to declare a dependency on this module. One way of adding a servlet filter in code is as follows. 

```java
List<ServletFilterConfiguration> servletFilterConfigurations = registryConfiguration.getServletFilters();
if (servletFilterConfigurations != null && !servletFilterConfigurations.isEmpty()) {
    for (ServletFilterConfiguration servletFilterConfiguration: servletFilterConfigurations) {
        try {
            FilterRegistration.Dynamic dynamic = environment.servlets().addFilter(servletFilterConfiguration.getClassName(), (Class<? extends Filter>)
            Class.forName(servletFilterConfiguration.getClassName()));
            dynamic.setInitParameters(servletFilterConfiguration.getParams());
            dynamic.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
        } catch (Exception e) {
            LOG.error("Error registering servlet filter {}", servletFilterConfiguration);
            throw new RuntimeException(e);
        }
    }
}
```

In the above code, ServletFilterConfiguration is a Java object representing the servlet filter specified using the registry YAML file as show in the example 
below. However the general idea is that one needs to add com.hortonworks.registries.auth.server.AuthenticationFilter for enabling authentication 

The filter configuration is passed using the params property in the YAML file, as follows:

```yaml
servletFilters:
 - className: "com.hortonworks.registries.auth.server.AuthenticationFilter"
   params:
     type: "kerberos"
     kerberos.principal: "HTTP/web-service-host.com"
     kerberos.keytab: "/path/to/keytab"
     kerberos.name.rules: "RULE:[2:$1@$0]([jt]t@.*EXAMPLE.COM)s/.*/$MAPRED_USER/ RULE:[2:$1@$0]([nd]n@.*EXAMPLE.COM)s/.*/$HDFS_USER/DEFAULT"
     token.validity: 36000
```

The servlet filter uses the principal `HTTP/{hostname}` to login(hostname must be the host where the web-service runs) . Make sure that principal is 
created as part of Kerberos setup

Once configured, the user must do `kinit` on client side using the principal declared before accessing the web-service via the browser or some other client. 
This principal also needs to be created first during Kerberos setup

Here's an example on how to access the web-service after the setup above:
```bash
curl  -i --negotiate -u:anyUser  -b ~/cookiejar.txt -c ~/cookiejar.txt  http://<web-service-host>:<port>/api/v1/
```

1. Firefox: Go to `about:config` and search for `network.negotiate-auth.trusted-uris` double-click to add value "http://<web-service-host>:<port>"
2. Google-chrome: start from command line with: 
   `google-chrome --auth-server-whitelist="*web-service-hostname" --auth-negotiate-delegate-whitelist="*web-service-hostname"`
3. IE: Configure trusted websites to include "web-service-hostname" and allow negotiation for that website

**Caution**: In AD MIT Kerberos setup, the key size is bigger than the default UI jetty server request header size. If using MIT Kerberos with jettty server, 
make sure you set HTTP header buffer bytes to 65536



