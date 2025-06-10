package top.cikipad.test;

import java.util.*;

class Solution {
    public void merge(int[] nums1, int m, int[] nums2, int n) {
        for (int i = 0; i < n; i++) {
            nums1[m + i] = nums2[i];
        }

        fastSort(nums1,0, nums1.length -1);
    }

    public void swap(int x, int y, int[] nums) {
        int tmp = nums[x];
        nums[x] = nums[y];
        nums[y] = tmp;
    }


    public void selectSort(int[] arr) {
        for (int i = 0; i< arr.length;i++) {
            int minIndex = i;
            for (int j = i;j< arr.length;j++) {
                if (arr[minIndex] > arr[j]) {
                    minIndex = j;
                }
            }
            swap(minIndex, i, arr);
        }
    }


    public void bubbleSort(int[] arr) {
        for (int i=0;i<arr.length - 1;i++) {
            for (int j=0;j<arr.length - 1 - i;j++) {
                if (arr[j] > arr[j+1] ) {
                    swap(j,j+1,arr);
                }
            }
        }
    }



    public void fastSort(int[] arr,int start, int end) {
        if (start >= end) {
            return;
        }
        int low = start;
        int high = end;
        while (low < high) {
            while(low<high && arr[high] >= arr[start]) {
                high--;
            }
            while(low<high && arr[low] <= arr[start]) {
                low++;
            }
            if (low<high) {
                int tmp = arr[low];
                arr[low] = arr[high];
                arr[high] = tmp;
            }
        }
        if (low != start) {
            int tmp = arr[low];
            arr[low] = arr[start];
            arr[start] = tmp;
        }
        fastSort(arr,start,low -1);
        fastSort(arr,low +1 , end);
    }


    public int removeElement(int[] nums, int val) {
        int count = 0;

        for (int i=0;i<nums.length;i++) {

            if (nums[i] == val) {
                count++;
                continue;
            }
            else {
                if (count!=0) {
                    nums[i-count] = nums[i];
                }
            }
        }
        return nums.length - count;
    }


//    public int removeDuplicates(int[] nums) {
//        if (nums.length <= 1) {
//            return nums.length;
//        }
//
//        int slow = 0;
//        int fast = 1;
//        while (fast<nums.length) {
//            if (nums[slow] != nums[fast]) {
//                nums[slow+1] = nums[fast];
//                slow++;
//            }
//            fast++;
//        }
//        return slow;
//    }


    //双指针
    public int removeDuplicates(int[] nums) {
        //边界条件
        if (nums.length < 3) {
            return nums.length;
        }

        int slow = 1;
        int fast = 2;

        while (fast<nums.length) {

            //满足重复条件时,slow不动
            if (slow>=1 && nums[slow] == nums[fast] && nums[slow-1] == nums[fast]) {

            }
            else{
                slow++;
                nums[slow] = nums[fast];
            }

            fast++;

        }

        return slow+1;

    }


    public int majorityElement(int[] nums) {
        for (int i=0;i<nums.length;i++) {
            boolean find = false;
            if (nums[i] == Integer.MIN_VALUE) {
                continue;
            }
            for (int j=i;j<nums.length;j++) {
                if (nums[j] == Integer.MIN_VALUE) {
                    continue;
                }
                if (nums[i] != nums[j]) {
                    nums[i] = Integer.MIN_VALUE;
                    nums[j] = Integer.MIN_VALUE;
                    find =true;
                    break;
                }
            }
            if (!find) {
                return nums[i];
            }
        }
        return Integer.MAX_VALUE;
    }

//    public static void main(String[] args) {
//        Solution s = new Solution();
//        int[] arr = new int[]{3,3,4};
//        System.out.println(s.majorityElement(arr));
//    }


    public void rotate(int[] nums, int k) {
        Deque<Integer> queue = new ArrayDeque<>();
        for (int num:nums) {
            queue.addLast(num);
        }
        for (int i=0;i<k;i++) {
            int tmp = queue.pollLast();
            queue.addFirst(tmp);
        }

        for (int i=0;i<nums.length;i++) {
            nums[i] = queue.pollFirst();
        }
    }


    public int maxProfit(int[] prices) {
        int min = Integer.MAX_VALUE;
        int res = 0;
        boolean rise = false;

        for (int i=0;i<prices.length;i++) {
            if (i+1<prices.length && prices[i+1] > prices[i]) {
                if (!rise) {
                    min = prices[i];
                }
                rise = true;
            }

            if (i+1<prices.length && prices[i+1] <= prices[i]) {
                if (rise) {
                    res += prices[i] - min;
                    min = Integer.MAX_VALUE;
                }
                rise=false;
            }

            if (i+1==prices.length && rise) {
                res += prices[i] - min;
            }
        }
        return res;
    }



    public boolean canJump(int[] nums) {
        int max = 0;
        for (int i = 0; i< nums.length;i++) {
            if (i > max) {
                return false;
            }

            if (i == nums.length - 1) {
                return true;
            }

            max = Math.max(i + nums[i], max);
        }
        return false;
    }




    public int jump(int[] nums) {
        int[] countArr = new int[nums.length];
        for (int i = 0; i< nums.length;i++) {
            if (countArr[i] == 0 && i!=0) {
                return Integer.MAX_VALUE;
            }

            for (int j=1;j<=nums[i];j++) {
                if (i+j>nums.length) {
                    break;
                }
                if (countArr[i+j] == 0) {
                    countArr[i+j] = countArr[i] + 1;
                }
                else {
                    countArr[i+j] = Math.min(countArr[i] +1,countArr[i+j]);
                }
            }

        }
        return nums[nums.length -1];
    }


    public int romanToInt(String s) {
        Map<Character, Integer> romanMap = new HashMap<>();
        romanMap.put('I', 1);
        romanMap.put('V', 5);
        romanMap.put('X', 10);
        romanMap.put('L', 50);
        romanMap.put('C', 100);
        romanMap.put('D', 500);
        romanMap.put('M', 1000);
        int res = 0;
        char[] charArr = s.toCharArray();
        for (int i = 0; i<charArr.length; i++) {
            int curVal = romanMap.get(charArr[i]);
            if (i != charArr.length - 1 && romanMap.get(charArr[i]) < romanMap.get(charArr[i+1])) {
                res -= curVal;
            }
            else {
                res += curVal;
            }
        }
        return res;
    }


    public String intToRoman(int num) {

        List<RomanChar> charList = new ArrayList<>();


        charList.add(new RomanChar("M", 1000 ,true));
        charList.add(new RomanChar("CM", 900 ,false));
        charList.add(new RomanChar("D", 500 ,false));
        charList.add(new RomanChar("CD", 400 ,false));
        charList.add(new RomanChar("C", 100 ,true));
        charList.add(new RomanChar("XC", 90 ,false));
        charList.add(new RomanChar("L", 50 ,false));
        charList.add(new RomanChar("XL", 40 ,false));
        charList.add(new RomanChar("X", 10 ,true));
        charList.add(new RomanChar("IX", 9 ,false));
        charList.add(new RomanChar("V", 5 ,false));
        charList.add(new RomanChar("IV", 4 ,false));
        charList.add(new RomanChar("I", 1 ,true));


        StringBuilder sb = new StringBuilder();
        int res = num;
        for (RomanChar romanChar:charList) {
            if (romanChar.repeat) {
                int repeatNum = res / romanChar.value;
                for (int i =0;i<repeatNum;i++) {
                    sb.append(romanChar.str);
                    res -= romanChar.value;
                }
            } else {
                int repeatNum = res / romanChar.value;
                if (repeatNum > 0) {
                    sb.append(romanChar.str);
                    res-= romanChar.value;
                }
            }
        }
        return sb.toString();
    }

    private class RomanChar {
        public String str;
        public int value;
        public boolean repeat;

        public RomanChar(String str,int value,boolean repeat) {
            this.str = str;
            this.value = value;
            this.repeat = repeat;
        }
    }


    public int lengthOfLastWord(String s) {
        char[] chars = s.toCharArray();
        boolean flag = false;
        int end = -1;
        int start = -1;
        for (int i=chars.length-1;i>=0;i--) {
            if (!flag) {
                if (chars[i] != ' ') {
                    end = i+1;
                    start = i;
                    flag = true;
                }
            }
            if (flag) {
                if (chars[i] == ' ') {
                    return end - start;
                }
                else {
                    start = i;
                }
            }
        }
        return end - start;
    }



    public String longestCommonPrefix(String[] strs) {
        if (strs.length == 0) {
            return "";
        }
        char[] chars = strs[0].toCharArray();
        int index = chars.length - 1;

        for (int i=1;i<strs.length;i++) {
            char[] cmp = strs[i].toCharArray();
            int count = -1;
            for (int j=0;j<=Math.min(index, cmp.length-1); j++) {
                if (cmp[j] == chars[j]) {
                    count = j;
                }
                else {
                    break;
                }
            }
            index = count;
        }

        StringBuilder sb = new StringBuilder();
        for (int i=0;i<=index;i++) {
            sb.append(chars[i]);
        }

        return sb.toString();

    }



    public String reverseWords(String s) {
        StringBuilder sb = new StringBuilder();
        String[] split = s.trim().split(" ");
        for (int i = split.length - 1; i >= 0; i--) {
            if (split[i].trim().length() > 0) {
                sb.append(split[i].trim());
                if (i != 0) {
                    sb.append(" ");
                }
            }
        }
        return sb.toString();
    }


    public String convert(String s, int numRows) {
        if (numRows <= 0) {
            return "";
        }

        if (numRows ==1) {
            return s;
        }

        List<StringBuilder> sbl = new ArrayList<>();
        for (int i=0;i<numRows;i++) {
            sbl.add(new StringBuilder());
        }

        int mod = 2 * (numRows - 1);
        char[] chars = s.toCharArray();
        for (int i=0;i<chars.length;i++) {
            int res = Math.abs(((i) % mod) - numRows +1);
            sbl.get(res).append(chars[i]);
        }

        StringBuilder sb = new StringBuilder();
        for (int i =numRows-1;i>=0;i--) {
            sb.append(sbl.get(i).toString());
        }
        return sb.toString();
    }


    public boolean isPalindrome(String s) {
        char[] chars = s.toLowerCase().toCharArray();
        int l = 0;
        int r = chars.length - 1;
        while (l<r) {
            while (l<r && !((chars[l]>='a' && chars[l] <='z') ||(chars[l] >='0' && chars[l] <= '9'))) {
                l++;
            }
            while (l<r && !((chars[r]>='a' && chars[r] <='z') ||(chars[r] >='0' && chars[r] <= '9'))) {
                r--;
            }
            if (l>=r) {
                return true;
            }
            if (chars[l] != chars[r]) {
                return false;
            }
            l++;
            r--;
        }
        return true;
    }


    public int strStr(String haystack, String needle) {
        return haystack.indexOf(needle);
    }



    public boolean isSubsequence(String s, String t) {

        if (s.length()==0) {
            return true;
        }
        if (s.length() > t.length()) {
            return false;
        }

        char[] chars1 = s.toCharArray();
        char[] chars2 = t.toCharArray();
        int index = 0;

        for (int i=0;i<chars2.length;i++) {
            if (chars1[index] == chars2[i]) {
                index++;
                if (index == chars1.length) {
                    return true;
                }
            }
        }
        return false;

    }


    public int[] twoSum(int[] numbers, int target) {
        if (numbers.length<2) {
            return new int[]{};
        }

        int l = 0;
        int r = numbers.length - 1;

        while (l<r) {
            int cur = numbers[l] + numbers[r];
            if (cur<target) {
                l++;
            }
            else if (cur>target) {
                r--;
            }
            else {
                return new int[]{l+1,r+1};
            }
        }
        return new int[]{};
    }



    public int maxArea(int[] height) {
        int l = 0;
        int r = height.length - 1;
        int max = Math.min(height[l], height[r]) * (r - l);
        while (l<r) {
            if (l<r && height[l] < height[r]) {
                l++;
            }
            else if (l<r && height[l] >= height[r]) {
                r--;
            }
            if (l<r) {
                max = Math.max(max, Math.min(height[l], height[r]) * (r - l));
            }
        }
        return max;
    }


    public List<List<Integer>> threeSum(int[] nums) {
        Arrays.sort(nums);
        List<List<Integer>> ret = new ArrayList<>();
        for (int i=0;i<nums.length - 2;i++) {

            if (i-1>=0 && nums[i] == nums[i-1]) {
                continue;
            }

            int target = nums[i];
            int l = i + 1;
            int r = nums.length - 1;

            int cur = nums[i] + nums[l] + nums[r];

            if (cur == 0) {
                List<Integer> one = new ArrayList<>();
                one.add(nums[i]);
                one.add(nums[l]);
                one.add(nums[r]);
                ret.add(one);
            }

            while(l<r) {
                cur = nums[i] + nums[l] + nums[r];
                if (l<r && cur > 0) {
                    r = right(l,r,nums);
                }
                else if (l<r && cur <0) {
                    l = left(l,r,nums);
                }
                else if (l<r && cur ==0){
                    l = left(l,r,nums);
                }

                if (l<r) {
                    if (nums[i] + nums[l] + nums[r] == 0) {
                        List<Integer> one = new ArrayList<>();
                        one.add(nums[i]);
                        one.add(nums[l]);
                        one.add(nums[r]);
                        ret.add(one);
                    }
                }
            }
        }
        return ret;
    }

    public int left(int l, int r, int[] nums) {
        int cur = nums[l];
        while (l<r && nums[l] == cur) {
            l++;
        }
        return l;
    }

    public int right(int l, int r ,int[] nums) {
        int cur = nums[r];
        while (l<r && nums[r] == cur) {
            r--;
        }
        return r;
    }


//    public int minSubArrayLen(int target, int[] nums) {
//        int min = Integer.MAX_VALUE;
//        for (int i=0;i< nums.length;i++) {
//            for (int j=nums.length-1;j>=i;j--) {
//                if (sum(i,j,nums) >= target) {
//                    min = Math.min(j-i+1, min);
//                }
//            }
//        }
//        return min==Integer.MAX_VALUE? 0:min;
//    }


    public int minSubArrayLen(int target, int[] nums) {
        int min = Integer.MAX_VALUE;
        int sum = nums[0];
        int slow = 0;
        int fast = 0;
        while (fast<nums.length && slow<=fast) {

            //慢指针找一个不合适的点
            if (sum >= target) {
                min = Math.min(fast - slow + 1, min);
                if (min == 1) {
                    return 1;
                }
                if (fast<nums.length && slow<=fast) {
                    sum -= nums[slow];
                }
                slow++;
            }
            //快指针找一个合适的点
            else {
                fast++;
                if (fast<nums.length && slow<=fast) {
                    sum += nums[fast];
                }
            }
        }

        return min==Integer.MAX_VALUE? 0:min;
    }

    private int sum(int start,int end, int[] nums) {
        int sum = 0;
        for (int i =start;i<=end;i++) {
            sum+=nums[i];
        }
        return sum;
    }


    public int lengthOfLongestSubstring(String s) {

        if (s ==null || s.length() ==0) {
            return 0;
        }
        int max = 0;
        int l = 0;
        int r = 0;
        char[] arr = s.toCharArray();
        Set<Character> record = new HashSet<>();
        while (l<=r && r<s.length()) {
            if (record.contains(arr[r])) {
                while (l<=r && record.contains(arr[r])) {
                    record.remove(arr[l]);
                    l++;
                }
            }
            else {
                max = Math.max(r-l+1, max);
                record.add(arr[r]);
                r++;
            }
        }
        return max;
    }

    public static void main(String[] args) {
        char[][] board = {
                {'5', '3', '.', '.', '7', '.', '.', '.', '.'},
                {'6', '.', '.', '1', '9', '5', '.', '.', '.'},
                {'.', '9', '8', '.', '.', '.', '.', '6', '.'},
                {'8', '.', '.', '.', '6', '.', '.', '.', '3'},
                {'4', '.', '.', '8', '.', '3', '.', '.', '1'},
                {'7', '.', '.', '.', '2', '.', '.', '.', '6'},
                {'.', '6', '.', '.', '.', '.', '2', '8', '.'},
                {'.', '.', '.', '4', '1', '9', '.', '.', '5'},
                {'.', '.', '.', '.', '8', '.', '.', '7', '9'}
        };
        Solution s = new Solution();
        System.out.println(s.isValidSudoku(board));
    }



    public boolean isValidSudoku(char[][] board) {
        Set<Character>[][] cell = new Set[3][3];
        Set<Character>[] line = new Set[9];
        Set<Character>[] row = new Set[9];

        for (int i=0;i<9;i++) {
            line[i] = new HashSet<>();
            row[i] = new HashSet<>();
        }
        for (int i=0;i<3;i++) {
            for (int j=0;j<3;j++) {
                cell[i][j] = new HashSet<>();
            }
        }

        for (int i = 0;i<9;i++) {
            for (int j=0;j<9;j++) {

                if (board[i][j] == '.') {
                    continue;
                }

                if (line[i].contains(board[i][j])) {
                    return false;
                }
                else {
                    line[i].add(board[i][j]);
                }

                if (row[j].contains(board[i][j])) {
                    return false;
                }
                else {
                    row[j].add(board[i][j]);
                }

                int x = (int)(i/3);
                int y = (int)(j/3);

                if (cell[x][y].contains(board[i][j])) {
                    return false;
                }
                else {
                    cell[x][y].add(board[i][j]);
                }
            }
        }
        return true;

    }
}