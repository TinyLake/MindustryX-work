package arc.graphics.g2d;

import arc.graphics.*;
import arc.math.*;
import mindustryX.features.*;

import java.util.*;

public class MySortedSpriteBatch extends SortedSpriteBatch2{
    private static final boolean DEBUG = false;
    private static final int PRIME1 = 0xbe1f14b1;
    private static final int PRIME2 = 0xb4b82e39;
    int[] extraZ = new int[10000];
    //增加小的delta，来保持原来的前后顺序
    int orderZ = 0;
    int hashZ = 0;//打乱hash值，来检查渲染异常

    @Override
    protected void z(float z){
        if(this.z == z) return;
        orderZ = 0;
        super.z(z);
    }

    @Override
    protected void flushRequests(){
        if(!flushing){
            DebugUtil.lastDrawRequests += numRequests;
            super.flushRequests();
        }
    }

    @Override
    protected void expandRequests(){
        super.expandRequests();
        extraZ = new int[requests.length];
    }

    @Override
    protected void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float rotation){
        super.draw(region, x, y, originX, originY, width, height, rotation);
        if(sort && !flushing && RenderExt.renderSort){
            int h = region.texture.hashCode();
            if(DEBUG){
                h = (h + hashZ) * PRIME1;
                h = h ^ (h >>> 16);
            }
            extraZ[numRequests - 1] = ((orderZ++) << 16) | (h & 0xffff);
        }
    }

    @Override
    protected void draw(Texture texture, float[] spriteVertices, int offset, int count){
        super.draw(texture, spriteVertices, offset, count);
        if(sort && !flushing && RenderExt.renderSort){
            int h = texture.hashCode();
            if(DEBUG){
                h = (h + hashZ) * PRIME1;
                h = h ^ (h >>> 16);
            }
            int[] extraZ = this.extraZ;
            int v = ((orderZ++) << 16) | (h & 0xffff);
            for(int i = (count - offset) / SPRITE_SIZE; i > 0; i--){
                extraZ[numRequests - i] = v;
            }
        }
    }

    @Override
    protected void draw(Runnable request){
        super.draw(request);
        if(sort && !flushing && RenderExt.renderSort){
            int h = DEBUG ? hashZ : 0;
            extraZ[numRequests - 1] = ((orderZ++) << 16) | (h & 0xffff);
        }
    }

    @Override
    protected void sortRequests(){
        if(!RenderExt.renderSort){
            super.sortRequests();
            return;
        }
        hashZ = DEBUG ? Float.floatToIntBits((float)Math.random()) : 0;

        int numRequests = this.numRequests;
        int[] arr = this.requestZ, extraZ = this.extraZ;
        sortMap(arr, numRequests);
        sortMap(extraZ, numRequests);
        for(int i = 0; i < numRequests; i++){
            arr[i] = (arr[i] << 16) | extraZ[i];
        }
        countingSortMap(arr, numRequests);//arr is loc now;

        if(copy.length < requests.length) copy = new DrawRequest[requests.length];
        final DrawRequest[] items = requests, dest = copy;
        for(int i = 0; i < numRequests; i++){
            dest[arr[i]] = items[i];
        }
    }

    private static final IntIntMap vMap = new IntIntMap(10000, 0.25f);
    private static int[] order = new int[1000];
    private static int[] order2 = new int[1000];

    /** 将输入arr映射到有序的0-n域 */
    private static int sortMap(int[] arr, int len){
        var map = MySortedSpriteBatch.vMap;
        int[] order = MySortedSpriteBatch.order;
        map.clear();
        int unique = 0;
        for(int i = 0; i < len; i++){
            int v = arr[i];
            int id = map.getOrPut(v, unique);
            arr[i] = id;//arr现在表示id
            if(id == unique){
                if(order.length <= unique){
                    order = Arrays.copyOf(order, unique << 1);
                }
                order[unique] = v;
                unique++;
            }
        }
        MySortedSpriteBatch.order = order;

        //对z值排序
        Arrays.sort(order, 0, unique);//order -> z

        //arr中储存order
        int[] order2 = MySortedSpriteBatch.order2;//id -> order
        if(order2.length < order.length){
            order2 = new int[order.length];
            MySortedSpriteBatch.order2 = order2;
        }
        for(int i = 0; i < unique; i++){
            order2[map.getOrPut(order[i], -1)] = i;
        }
        for(int i = 0; i < len; i++){
            arr[i] = order2[arr[i]];
        }
        return unique;
    }

    private static void countingSortMap(int[] arr, int len){
        int[] order = MySortedSpriteBatch.order, counts = MySortedSpriteBatch.order2;
        var map = MySortedSpriteBatch.vMap;
        map.clear();
        int unique = 0;
        for(int i = 0; i < len; i++){
            int v = arr[i];
            int id = map.getOrPut(v, unique);
            arr[i] = id;//arr现在表示id
            if(id == unique){
                if(unique >= counts.length){
                    order = Arrays.copyOf(order, unique << 1);
                    counts = Arrays.copyOf(counts, unique << 1);
                    MySortedSpriteBatch.order = order;
                    MySortedSpriteBatch.order2 = counts;
                }
                order[unique] = v;
                counts[unique] = 1;
                unique++;
            }else counts[id]++;
        }

        //对z值排序
        Arrays.sort(order, 0, unique);//order -> z

        //将counts转换为locs
        int loc = 0;
        for(int i = 0; i < unique; i++){
            int id = map.getOrPut(order[i], -1);
            int c = counts[id];
            counts[id] = loc;
            loc += c;
        }
        //arr现在表示新目的地
        for(int i = 0; i < len; i++){
            arr[i] = counts[arr[i]]++;
        }
    }

    static public class IntIntMap{
        private int[] keys;
        private boolean hasZero;
        private int[] values;
        private int zeroValue;
        private int size; // 哈希表中的元素数量

        private int capacity, maxSize;
        private float loadFactor;
        private int mask, hashShift;

        public IntIntMap(int capacity, float loadFactor){
            setCapacity(capacity, loadFactor);
        }

        private int hash(int key){
            key *= PRIME2;
            return (key ^ key >>> hashShift);
        }

        public int getOrPut(int key, int defaultValue){
            if(key == 0){
                if(hasZero) return zeroValue;
                zeroValue = defaultValue;
                hasZero = true;
                return defaultValue;
            }
            int mask = this.mask;
            int index = hash(key) & mask;
            int[] keys = this.keys;
            while(keys[index] != 0){
                if(keys[index] == key){// 键找到
                    return values[index];
                }
                index = (index + 1) & mask;
            }
            //键不存在
            keys[index] = key;
            values[index] = defaultValue;
            size++;
            if(size > maxSize) setCapacity(capacity << 1, loadFactor);
            return defaultValue;
        }

        private void setCapacity(int capacity, float loadFactor){
            capacity = Mathf.nextPowerOfTwo(capacity);
            this.capacity = capacity;
            this.loadFactor = loadFactor;
            maxSize = (int)(capacity * loadFactor);
            int mask = this.mask = capacity - 1;
            hashShift = 31 - Integer.numberOfTrailingZeros(capacity);

            int[] oldKeys = keys, oldValues = values;
            int[] keys = this.keys = new int[capacity];
            int[] values = this.values = new int[capacity];
            if(oldKeys == null || oldValues == null) return;
            for(int i = 0; i < oldKeys.length; i++){
                if(oldKeys[i] == 0) continue;
                int index = hash(oldKeys[i]) & mask;
                while(keys[index] != 0){
                    index = (index + 1) & mask;
                }
                keys[index] = oldKeys[i];
                values[index] = oldValues[i]; // 插入或更新值
            }
        }

        private void clear(){
            Arrays.fill(keys, 0);
            Arrays.fill(values, 0);
            size = 0;
            hasZero = false;
        }
    }
}
