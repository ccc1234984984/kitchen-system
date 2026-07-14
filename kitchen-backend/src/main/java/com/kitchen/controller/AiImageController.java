package com.kitchen.controller;

import com.aliyun.oss.OSS;
import com.kitchen.common.Result;
import com.kitchen.config.OSSConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AI 文生图：根据菜品名生成图片，转存到 OSS 后返回永久地址。
 */
@RestController
@RequestMapping("/api/ai")
public class AiImageController {

    @Value("${ai.image.endpoint}")
    private String aiEndpoint;

    @Value("${ai.image.api-key}")
    private String aiApiKey;

    @Value("${ai.image.model}")
    private String aiModel;

    @Value("${ai.image.size}")
    private String aiSize;

    @Autowired
    private OSS ossClient;

    @Autowired
    private OSSConfig ossConfig;

    @PostMapping("/generate-dish-image")
    public Result<String> generateDishImage(@RequestBody Map<String, String> body) {
        String name = body == null ? null : body.get("name");
        if (name == null || name.trim().isEmpty()) {
            return Result.error("菜品名称不能为空");
        }
        name = name.trim();

        try {
            // 1. 调用 AI 生成图片，拿到临时图片地址
            String tempImageUrl = requestAiImage(name);
            if (tempImageUrl == null) {
                return Result.error("AI 未返回图片，请重试");
            }

            // 2. 下载图片字节
            RestTemplate restTemplate = new RestTemplate();
            byte[] imageBytes = restTemplate.getForObject(URI.create(tempImageUrl), byte[].class);
            if (imageBytes == null || imageBytes.length == 0) {
                return Result.error("下载 AI 图片失败");
            }

            // 3. 转存到 OSS，使用唯一文件名避免覆盖
            String key = "images/ai_" + UUID.randomUUID().toString().replace("-", "") + ".png";
            ossClient.putObject(ossConfig.getBucketName(), key, new ByteArrayInputStream(imageBytes));

            String bucketDomain = "https://" + ossConfig.getBucketName() + "."
                    + ossConfig.getEndpoint().replace("https://", "").replace("http://", "");
            String url = bucketDomain + "/" + key;
            return Result.success(url);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("生成失败：" + e.getMessage());
        }
    }

    /**
     * 调用 OpenAI 兼容的文生图接口，返回临时图片 URL。
     */
    @SuppressWarnings("unchecked")
    private String requestAiImage(String dishName) {
        RestTemplate restTemplate = new RestTemplate();

        String prompt = "一盘" + dishName + "，中式菜品，专业美食摄影，摆盘精致，自然光，干净的浅色背景，高清细节";

        Map<String, Object> reqBody = new HashMap<>();
        reqBody.put("model", aiModel);
        reqBody.put("prompt", prompt);
        reqBody.put("n", 1);
        reqBody.put("size", aiSize);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + aiApiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(reqBody, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(aiEndpoint, entity, Map.class);
        Map<String, Object> resBody = response.getBody();
        if (resBody == null) {
            return null;
        }

        Object dataObj = resBody.get("data");
        if (!(dataObj instanceof List)) {
            return null;
        }
        List<Object> dataList = (List<Object>) dataObj;
        if (dataList.isEmpty()) {
            return null;
        }
        Object first = dataList.get(0);
        if (!(first instanceof Map)) {
            return null;
        }
        Object urlObj = ((Map<String, Object>) first).get("url");
        return urlObj == null ? null : urlObj.toString();
    }
}
