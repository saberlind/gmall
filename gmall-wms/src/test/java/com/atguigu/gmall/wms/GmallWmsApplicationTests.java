package com.atguigu.gmall.wms;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

//@SpringBootTest
class GmallWmsApplicationTests {


    @Test
    void contextLoads() {
        int count = 0;
        int a[] = new int[]{1,5,3,9,13,26,12,2,6};
        for (int i = 0; i < a.length - 1; i++) {
            boolean flag = true; //假设已经不再进行交换
            for (int j = 0; j < a.length - 1 - i; j++) {
                count ++;
                if (a[j]>a[j+1]){
                    flag = false;
                    int temp = a[j];
                    a[j] = a[j+1];
                    a[j+1] = temp;
                }
            }
            if (flag){
                break;
            }
        }
        for (int i = 0; i < a.length; i++) {
            System.out.println(a[i]+" ");
        }
        System.out.println(Arrays.toString(a));
        System.out.println("比较了" + count);
    }

    @Test
    void test1(){
        int[] nums = {6, 5, 3, 1, 8, 7, 2, 4};
        int count = 0;
        for (int j = 0; j < nums.length - 1; j++) {
            for (int i = 0; i < nums.length - 1; i++) {
                count++;
                if (nums[i] > nums[i + 1]) {
                    int tmp = nums[i];
                    nums[i] = nums[i + 1];
                    nums[i + 1] = tmp;
                }
            }
            System.out.println("第" + j + "趟的结果" + Arrays.toString(nums));
        }
        System.out.println("比较了" + count + "次");
    }

    @Test
    void test2(){
        int[] nums = {6, 5, 3, 1, 8, 7, 2, 4};
        int count = 0;
        for (int j = 0; j < nums.length - 1; j++) {  // j =0; j = 2
            for (int i = 0; i < nums.length - 1 - j; i++) {  // i < 5
                count++;
                if (nums[i] > nums[i + 1]) {
                    int tmp = nums[i];
                    nums[i] = nums[i + 1];
                    nums[i + 1] = tmp;
                }
            }
            System.out.println("第" + j + "趟的结果" + Arrays.toString(nums));
        }
        System.out.println("比较了" + count + "次");
    }

    @Test
    int testMid(){
        int srcArray[] = {1,5,7,9,13,21,32};
        int des = 0;
        //定义初始最小、最大索引
        int start = 0;
        int end = srcArray.length - 1;
        //确保不会出现重复查找，越界
        while (start <= end) {
            //计算出中间索引值
            int middle = (end + start)>>>1 ;//防止溢出
            if (des == srcArray[middle]) {
                return middle;
                //判断下限
            } else if (des < srcArray[middle]) {
                end = middle - 1;
                //判断上限
            } else {
                start = middle + 1;
            }
        }
        //若没有，则返回-1
        return -1;
    }
}
