/**
 * @author dmego
 * @date 2022/04/27 20:20
 */

public class Test {

    static class Man {
        private int size;

        private Son inter;

        public Son getInter() {
            return new Son();
        }

        class Son {
            private int sonSize = size;
        }
    }

    public static void main(String[] args) {
        Object[] arr = new Object[2];
        Man man = new Man();
        man.size = 10;
        arr[0] = man;

        Class<?> aClass = arr[0].getClass();
        System.out.println(aClass.getName());


        int sonSize = man.getInter().sonSize;
        System.out.println(sonSize);

    }

}
