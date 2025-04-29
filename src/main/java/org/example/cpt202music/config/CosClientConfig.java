package org.example.cpt202music.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "cos.client")
@Data
public class CosClientConfig {  
  
    /**  
     * Domain  
     */  
    private String host;  
  
    /**  
     * secretId  
     */  
    private String secretId;  
  
    /**  
     * Secret key (be careful not to expose it)  
     */  
    private String secretKey;  
  
    /**  
     * Region  
     */  
    private String region;  
  
    /**  
     * Bucket name  
     */  
    private String bucket;  
  
    @Bean
    public COSClient cosClient() {
        // Initialize user identity information (secretId, secretKey)  
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        // Set the region of the bucket, for COS region abbreviations please refer to https://www.qcloud.com/document/product/436/6224  
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        // Generate COS client  
        return new COSClient(cred, clientConfig);  
    }  
}
