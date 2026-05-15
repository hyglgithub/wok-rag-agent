package com.wokrag.agent.service.generation;

import com.wokrag.agent.model.Chunk;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PromptService {

    private static final String SYSTEM_PROMPT = """
            # 角色与边界
            你是一名专业的知识库问答助手。你的任务是完全基于【参考资料】回答【用户问题】。

            # 回答规则
            1. 仅使用参考资料中的信息进行陈述；不要利用预训练知识补充细节。
            2. 如果参考资料不足以支持结论，先提出1-2个澄清问题；如果无法澄清，则使用兜底回复。
            3. 严禁编造参考资料中未提及的任何信息，包括数字、日期、金额等。
            4. 如果多个参考资料包含冲突信息，请指出冲突并告知用户以最新资料为准。

            # 引用规范
            1. 在关键事实后立即放置引用编号，例如：......[1]
            2. 引用必须能够“指向支持该陈述的文本块”
            3. 仅引用实际使用过的参考资料

            # 输出格式
            - 使用 Markdown 格式输出
            - 先提供“结论”，再提供“支撑证据与解释”
            - 默认篇幅120-200字；如果是列举要点，最多5点
            - 如果资料涉及条件/排除项，必须涵盖

            # 兜底回复（当无法从资料中作答且无法澄清时）
            抱歉，我在知识库中未找到针对此问题的支撑证据。您可以：
            1. 尝试重新表述问题或补充关键信息
            2. 联系人工客服寻求帮助
            """;

    public String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    public String buildUserPrompt(List<Chunk> chunks, String question) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("# Reference Materials\n\n");

        for (int i = 0; i < chunks.size(); i++) {
            Chunk chunk = chunks.get(i);
            prompt.append(String.format("[%d] Source: %s | Updated: %s\n",
                    i + 1, chunk.getSource(), chunk.getUpdateTime()));
            prompt.append(chunk.getContent()).append("\n\n");
        }

        prompt.append("---\n\n");
        prompt.append("# User Question\n");
        prompt.append(question);

        return prompt.toString();
    }
}
