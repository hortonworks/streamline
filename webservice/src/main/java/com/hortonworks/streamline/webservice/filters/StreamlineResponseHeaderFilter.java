/**
 * Copyright 2017 Hortonworks.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 *   http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

package com.hortonworks.streamline.webservice.filters;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class StreamlineResponseHeaderFilter implements Filter {
  private final Map<String, String> headers = new HashMap<>();

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    Enumeration<String> names = filterConfig.getInitParameterNames();
    while (names.hasMoreElements()) {
      String key = names.nextElement();
      headers.put(key, filterConfig.getInitParameter(key));
    }
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    if (response instanceof HttpServletResponse) {
      HttpServletResponse httpServletResponse = ((HttpServletResponse) response);
      for (Map.Entry<String, String> header : headers.entrySet()) {
        httpServletResponse.setHeader(header.getKey(), header.getValue());
      }
      chain.doFilter(request, httpServletResponse);
    }
  }

  @Override
  public void destroy() {

  }
}
