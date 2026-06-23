package com.example.demo._xss;

import com.example.demo.utils.JsoupUtil;
import org.springframework.util.StringUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.Part;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Jsoup过滤http请求，防止Xss攻击
 *
 */
public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {

    HttpServletRequest orgRequest = null;
    
    private boolean isIncludeRichText = false;
    
    public XssHttpServletRequestWrapper(HttpServletRequest request, boolean isIncludeRichText) {
        super(request);
        orgRequest = request;
        this.isIncludeRichText = isIncludeRichText;
    }

    /**
    * 覆盖getParameter方法，将参数名和参数值都做xss过滤如果需要获得原始的值，则通过super.getParameterValues(name)来获取
    * getParameterNames,getParameterValues和getParameterMap也可能需要覆盖
    */
    @Override
    public String getParameter(String name) {
        if (("content".equals(name) || name.endsWith("WithHtml")) && !isIncludeRichText) {
            return super.getParameter(name);
        }
        name = JsoupUtil.clean(name);
        String value = super.getParameter(name);
        if (!StringUtils.isEmpty(value)) {
            value = JsoupUtil.clean(value);
        }
        return value;
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] arr = super.getParameterValues(name);
        if (arr != null) {
            for (int i = 0; i < arr.length; i++) {
                arr[i] = JsoupUtil.clean(arr[i]);
            }
        }
        return arr;
    }

    /**
    * 覆盖getHeader方法，将参数名和参数值都做xss过滤如果需要获得原始的值，则通过super.getHeaders(name)来获取
    * getHeaderNames 也可能需要覆盖
    */
    @Override
    public String getHeader(String name) {
        name = JsoupUtil.clean(name);
        String value = super.getHeader(name);
        if (!StringUtils.isEmpty(value)) {
            value = JsoupUtil.clean(value);
        }
        return value;
    }

    /**
     * 覆盖 getParts 方法，对文件上传进行XSS检查
     */
    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        Collection<Part> parts = super.getParts();
        List<Part> validatedParts = new ArrayList<>();

        for (Part part : parts) {
            validatePart(part);
            validatedParts.add(part);
        }

        return validatedParts;
    }

    /**
     * 覆盖 getPart 方法，对单个文件进行XSS检查
     */
    @Override
    public Part getPart(String name) throws IOException, ServletException {
        Part part = super.getPart(name);
        if (part != null) {
            validatePart(part);
        }
        return part;
    }

    /**
     * 验证上传的文件部分，检查文件名和内容
     */
    private void validatePart(Part part) throws IOException {
        // 1. 检查文件名是否包含XSS攻击代码
        String submittedFileName = part.getSubmittedFileName();
        if (submittedFileName != null && !submittedFileName.isEmpty()) {
            if (containsXss(submittedFileName)) {
                throw new RuntimeException("文件名包含非法字符: " + submittedFileName);
            }
        }

        // 2. 检查文件内容（仅针对文本类型的文件）
        String contentType = part.getContentType();
        if (contentType != null && isTextBasedContent(contentType)) {
            byte[] content = readAllBytes(part.getInputStream());
            String contentStr = new String(content, StandardCharsets.UTF_8);
            if (containsXss(contentStr)) {
                throw new RuntimeException("文件内容包含潜在的XSS攻击代码");
            }
        }
    }

    private byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[4096];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }

    /**
     * 判断是否为基于文本的内容类型
     */
    private boolean isTextBasedContent(String contentType) {
        String lowerType = contentType.toLowerCase();
        return lowerType.contains("svg")
                || lowerType.contains("html")
                || lowerType.contains("xml")
                || lowerType.contains("json")
                || lowerType.contains("text/")
                || lowerType.contains("javascript");
    }

    /**
     * 检查字符串是否包含XSS攻击特征
     */
    private boolean containsXss(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }

        String lowerValue = value.toLowerCase();

        // 检查常见的XSS攻击模式
        return lowerValue.contains("<script")
                || lowerValue.contains("</script>")
                || lowerValue.contains("javascript:")
                || lowerValue.contains("vbscript:")
                || lowerValue.contains("onerror=")
                || lowerValue.contains("onload=")
                || lowerValue.contains("onclick=")
                || lowerValue.contains("onmouseover=")
                || lowerValue.contains("onfocus=")
                || lowerValue.contains("eval(")
                || lowerValue.contains("expression(")
                || lowerValue.contains("url(")
                || lowerValue.contains("import ")
                || lowerValue.contains("<iframe")
                || lowerValue.contains("<object")
                || lowerValue.contains("<embed")
                || lowerValue.contains("<applet");
    }

    /**
    * 获取原始的request
    */
    public HttpServletRequest getOrgRequest() {
        return orgRequest;
    }

    /**
    * 获取原始的request的静态方法
    */
    public static HttpServletRequest getOrgRequest(HttpServletRequest req) {
        if (req instanceof XssHttpServletRequestWrapper) {
            return ((XssHttpServletRequestWrapper) req).getOrgRequest();
        }
        return req;
    }

}