package com.atguigu.gmall.auth;

import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.common.utils.RsaUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

public class JwtTest {

    // 别忘了创建D:\\project\rsa目录
	private static final String pubKeyPath = "D:\\at_Java\\rsa\\rsa.pub";
    private static final String priKeyPath = "D:\\at_Java\\rsa\\rsa.pri";

    private PublicKey publicKey;

    private PrivateKey privateKey;

    @Test
    public void testRsa() throws Exception {
        RsaUtils.generateKey(pubKeyPath, priKeyPath, "30489ouerweljrLROE@#)(@$*343jl");
    }

    @BeforeEach
    public void testGetRsa() throws Exception {
        this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
    }

    @Test
    public void testGenerateToken() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("id", "001");
        map.put("username", "saberlind");
        // 生成token
        String token = JwtUtils.generateToken(map, privateKey, 2);
        System.out.println("token = " + token);
    }

    @Test
    public void testParseToken() throws Exception {
        String token = "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6IjAwMSIsInVzZXJuYW1lIjoic2FiZXJsaW5kIiwiZXhwIjoxNjM5NzM1MzgxfQ.YR8Zbd5HW5s063CtxmxIw-a9eXPoDFCwoN4CBrPm3W6pagGrP1_QA7JAepDTAnklhacsCIEs-MvvV5tS_rY4OR2-E7HIRn1tVsVkSD1tsBOn3ibmHpumq1C0e-m_PFv-N6ycwuLZmt_yqcamPYSjBI35LxYDH7M02IMgoJgy1_hnWnX0-Vk1MDQ20GBu1DkXpmD3tzTxOGtNktBT-wi9FEAzoe26FmhPaqhxSfT8HOITGbSd2q4OaTsuq2HvDIcX-2QLg0zSkKAPm5RnTr_CcbQIJTDghH11hDYWVmMlBGg7FXgV6DRi0FaMMZ_bymFYijuD3vVEB90EudR-fiDJRg";

        // 解析token
        Map<String, Object> map = JwtUtils.getInfoFromToken(token, publicKey);
        System.out.println("id: " + map.get("id"));
        System.out.println("userName: " + map.get("username"));
    }
}