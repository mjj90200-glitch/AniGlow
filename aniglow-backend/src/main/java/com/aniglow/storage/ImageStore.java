package com.aniglow.storage;

/**
 * 图片对象存储抽象：隔离「图片字节落到哪里」的细节。
 * 实现方：本地磁盘（LocalImageStore，默认，开发兜底）与阿里云 OSS（AliyunOssImageStore）。
 * 切换供应商/实现只需配置 aniglow.storage.type，业务代码不感知。
 */
public interface ImageStore {

    /**
     * 保存图片字节。
     * @param data 图片字节（已完成校验/缩放/压缩）
     * @param key  相对键名，形如 2026/05/{uuid}.jpg
     * @return 可公开访问的 URL（本地模式为相对路径 /api/images/...，OSS 模式为绝对地址）
     */
    String store(byte[] data, String key);

    /** 该键是否已存在于本存储中 */
    boolean exists(String key);

    /** 解析键对应的公开访问 URL */
    String url(String key);

    /** 存储类型标识：local / oss */
    String type();
}
