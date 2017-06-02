package com.kang.solrj.service;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kang.solrj.pojo.Item;

public class ItemDataImportTest {

    private HttpSolrServer httpSolrServer;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Before
    public void setUp() throws Exception {
        // 在url中指定core名称：enjoyshop
        // http://solr.enjoyshop.com/#/enjoyshop -- 界面操作
        String url = "http://solr.enjoyshop.com/enjoyshop"; // 服务地址
        HttpSolrServer httpSolrServer = new HttpSolrServer(url); // 定义solr的server
        httpSolrServer.setParser(new XMLResponseParser()); // 设置响应解析器
        httpSolrServer.setMaxRetries(1); // 设置重试次数，推荐设置为1
        httpSolrServer.setConnectionTimeout(500); // 建立连接的最长时间

        this.httpSolrServer = httpSolrServer;
    }

    @Test
    public void testData() throws Exception {
        // 通过后台系统的接口查询商品数据
        String url = "http://manage.enjoyshop.com/rest/item?page={page}&rows=100";
        int page = 1;
        int pageSzie = 0;
        do {
            String u = StringUtils.replace(url, "{page}", "" + page);
            System.out.println(u);
            String jsonData = doGet(u);
            JsonNode jsonNode = MAPPER.readTree(jsonData);
            String rowsStr = jsonNode.get("rows").toString();
            List<Item> items = MAPPER.readValue(rowsStr,
                    MAPPER.getTypeFactory().constructCollectionType(List.class, Item.class));
            pageSzie = items.size();
            this.httpSolrServer.addBeans(items);
            this.httpSolrServer.commit();

            page++;
        } while (pageSzie == 100);

    }

    private String doGet(String url) throws Exception {
        // 创建Httpclient对象
        CloseableHttpClient httpclient = HttpClients.createDefault();

        // 创建http GET请求
        HttpGet httpGet = new HttpGet(url);

        CloseableHttpResponse response = null;
        try {
            // 执行请求
            response = httpclient.execute(httpGet);
            // 判断返回状态是否为200
            if (response.getStatusLine().getStatusCode() == 200) {
                return EntityUtils.toString(response.getEntity(), "UTF-8");
            }
        } finally {
            if (response != null) {
                response.close();
            }
            httpclient.close();
        }
        return null;
    }

}
