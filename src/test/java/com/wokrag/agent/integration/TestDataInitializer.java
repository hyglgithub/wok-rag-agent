package com.wokrag.agent.integration;

import com.wokrag.agent.service.embedding.EmbeddingService;
import com.wokrag.agent.service.retrieval.MilvusService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

@SpringBootTest
@ActiveProfiles("dev")
class TestDataInitializer {

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private MilvusService milvusService;

    @Test
    void insertTestData() {
        // Test data: customer service FAQ chunks
        List<String> texts = List.of(
                "退货政策：自收到商品之日起7天内，如商品未使用、包装完好，可申请无理由退货。退货时请保留原始包装和发票。退货流程：1.在订单详情页申请退货 2.等待审核（1个工作日内） 3.审核通过后寄回商品 4.收到商品后3-5个工作日内退款。",
                "配送时间：普通快递3-5个工作日送达，顺丰快递1-2个工作日送达。偏远地区可能额外增加1-2天。下单后24小时内发货，节假日顺延。可在订单详情页查看物流信息。",
                "支付方式：支持微信支付、支付宝、银行卡支付、花呗分期。暂不支持货到付款。分期付款可选3期、6期、12期，手续费分别为0%、3%、6%。",
                "会员权益：普通会员享受9.5折优惠，银卡会员（消费满1000元）享受9折优惠，金卡会员（消费满5000元）享受8.5折优惠。会员积分可兑换优惠券，100积分=1元。",
                "发票开具：下单时可选择开具电子发票或纸质发票。电子发票将在订单完成后24小时内发送至邮箱。纸质发票随商品一同寄出。发票内容可选：个人或公司。",
                "质保政策：电子产品享受1年质保，家电产品享受3年质保，服装类享受30天无理由退换。质保期内非人为损坏免费维修。需提供购买凭证和质保卡。"
        );

        System.out.println("=== Step 1: Generating embeddings ===");
        List<double[]> embeddings = embeddingService.embedBatch(texts);
        System.out.println("Generated " + embeddings.size() + " embeddings, dim=" + embeddings.get(0).length);

        // Build rows for Milvus insertion
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < texts.size(); i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("chunk_text", texts.get(i));
            row.put("doc_id", "faq-001");

            // Convert double[] to List<Float> for Milvus
            List<Float> floatVector = new ArrayList<>();
            for (double d : embeddings.get(i)) {
                floatVector.add((float) d);
            }
            row.put("text_dense", floatVector);
            row.put("source", "customer_service_faq.txt");
            row.put("source_url", "/docs/customer_service_faq.txt");

            rows.add(row);
        }

        System.out.println("=== Step 2: Inserting into Milvus ===");
        long insertCount = milvusService.insertChunks(rows);
        System.out.println("Inserted " + insertCount + " chunks into Milvus");

        assert insertCount == texts.size() : "Expected " + texts.size() + " inserts, got " + insertCount;
        System.out.println("=== Test data insertion completed successfully ===");
    }
}
