package com.dong.dongpicturebackendcommon.utils;

import java.awt.*;

/**
 * @author by hongdou
 * @date 2025/9/23.
 * @DESC: 计算图片相似度
 */
public class ColorSimlarUtils {
    private ColorSimlarUtils() {
        // 工具类不需要实例化
    }

    /**
     * 计算两个颜色的相似度
     * @param color1
     * @param color2
     * @return 相似度处于0-1之间，1表示完全相同，0表示完全不同
     */
    public static double calculateColorSimilarity(Color color1, Color color2){
        // 获取rgb值
        int r1 = color1.getRed();
        int g1 = color1.getGreen();
        int b1 = color1.getBlue();

        int r2 = color2.getRed();
        int g2 = color2.getGreen();
        int b2 = color2.getBlue();

        // 计算欧式距离
        double distance = Math.sqrt(Math.pow(r1 -r2, 2)
        + Math.pow(g1 - g2, 2) + Math.pow(b1 - b2, 2));

        // 计算相似度
        return 1 - distance / Math.sqrt(3 * Math.pow(252, 2));
    }

    /**
     * 根据十六进制颜色值计算颜色相似度
     * @param hexColor1
     * @param hexColor2
     * @return
     */
    public static double calculateColorSimilarity(String hexColor1, String  hexColor2){
        Color color1 = Color.decode(hexColor1);
        Color color2 = Color.decode(hexColor2);
        return calculateColorSimilarity(color1, color2);
    }

    public static void main(String[] args) {
        String color1 = "#FF5733"; // Example hex color 1
        String color2 = "#33FF57"; // Example hex color 2

        double similarity = calculateColorSimilarity(color1, color2);
        System.out.println("Color Similarity: " + similarity);
    }
}
