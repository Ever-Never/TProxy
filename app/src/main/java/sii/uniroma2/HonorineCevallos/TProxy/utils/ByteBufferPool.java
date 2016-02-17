package sii.uniroma2.HonorineCevallos.TProxy.utils;

/*
** Copyright 2015, Mohamed Naufal
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ByteBufferPool
{
    /*TODO: si dovrebbe esperimentare di più per scoprire il valore ideale di MSS del proxy
     * In tale esperimentazione bisogna tener conto sia dei problemi riguardanti la comunicazione
     * proxy -> App, sia i problemi ricguardanti la comunicazione proxy -> Remote Server*/
    private static final int PROXY_MSS = 16384;
    private static ConcurrentLinkedQueue<ByteBuffer> pool = new ConcurrentLinkedQueue<>();

    public static ByteBuffer acquire()
    {
        ByteBuffer buffer = pool.poll();
        if (buffer == null)
            buffer = ByteBuffer.allocateDirect(PROXY_MSS); // Using DirectBuffer for zero-copy
        return buffer;
    }

    public static ByteBuffer acquire(int bufferSize)
    {
        ByteBuffer buffer = pool.poll();
        if (buffer == null)
            buffer = ByteBuffer.allocateDirect(bufferSize); // Using DirectBuffer for zero-copy
        return buffer;
    }

    public static void release(ByteBuffer buffer)
    {
        buffer.clear();
        pool.offer(buffer);
    }

    public static void clear()
    {
        pool.clear();
    }
}
