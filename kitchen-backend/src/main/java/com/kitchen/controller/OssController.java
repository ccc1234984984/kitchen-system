package com.kitchen.controller;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectListing;
import com.aliyun.oss.model.OSSObjectSummary;
import com.kitchen.common.Result;
import com.kitchen.config.OSSConfig;
import com.kitchen.entity.Dish;
import com.kitchen.service.DishService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/oss")
public class OssController {

    @Autowired
    private OSS ossClient;

    @Autowired
    private OSSConfig ossConfig;

    @Autowired
    private DishService dishService;

    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }
        try {
            String originalName = file.getOriginalFilename();
            if (originalName == null || originalName.isEmpty()) {
                return Result.error("文件名不能为空");
            }
            String key = "images/" + originalName;
            ossClient.putObject(ossConfig.getBucketName(), key, file.getInputStream());
            String bucketDomain = "https://" + ossConfig.getBucketName() + "." + ossConfig.getEndpoint().replace("https://", "").replace("http://", "");
            String url = bucketDomain + "/" + key;
            return Result.success(url);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("上传失败：" + e.getMessage());
        }
    }

    @PostMapping("/sync-images")
    public Result<String> syncImages() {
        ObjectListing listing = ossClient.listObjects(ossConfig.getBucketName(), "images/");
        List<OSSObjectSummary> summaries = listing.getObjectSummaries();

        Map<String, String> nameToUrl = new HashMap<>();
        String bucketDomain = "https://" + ossConfig.getBucketName() + "." + ossConfig.getEndpoint().replace("https://", "").replace("http://", "");

        for (OSSObjectSummary s : summaries) {
            String key = s.getKey();
            String fileName = key.substring(key.lastIndexOf('/') + 1);
            String nameNoExt = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
            String url = bucketDomain + "/" + key;
            nameToUrl.put(nameNoExt, url);
        }

        List<Dish> dishes = dishService.listAll();
        int updated = 0;

        for (Dish dish : dishes) {
            String url = nameToUrl.get(dish.getName());
            if (url != null) {
                dish.setImageUrl(url);
                dishService.updateById(dish);
                updated++;
            }
        }

        return Result.success("同步完成，已更新 " + updated + " 个菜品图片");
    }
}
