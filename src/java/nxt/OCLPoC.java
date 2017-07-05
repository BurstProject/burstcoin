package nxt;

import nxt.util.Logger;
import nxt.util.MiningPlot;
import org.jocl.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;

import static org.jocl.CL.*;

final class OCLPoC {

    private static final int DEFAULT_MEM_PERCENT = 50;

    private static final int hashesPerEnqueue = Nxt.getIntProperty("burst.oclHashesPerEnqueue") == 0 ? 1000 : Nxt.getIntProperty("burst.oclHashesPerEnqueue");
    private static final int memPercent = Nxt.getIntProperty("burst.oclMemPercent") == 0 ? DEFAULT_MEM_PERCENT : Nxt.getIntProperty("burst.oclMemPercent");

    private static cl_context ctx;
    private static cl_command_queue queue;
    private static cl_program program;
    private static cl_kernel genKernel;
    private static cl_kernel getKernel;

    private static long maxItems;
    private static long maxGroupItems;

    private static final Object oclLock = new Object();

    private static final long memPerItem = 8 // id
                                + 8 // nonce
                                + MiningPlot.PLOT_SIZE + 16 // buffer
                                + 4 // scoop num
                                + MiningPlot.SCOOP_SIZE; // output scoop

    private static final long bufferPerItem = MiningPlot.PLOT_SIZE + 16;

    static void init() {}

    static {
        try {
            boolean autoChoose = Nxt.getBooleanProperty("burst.oclAuto", true);
            setExceptionsEnabled(true);

            int platformIndex;
            int deviceIndex;
            if(autoChoose) {
                AutoChooseResult ac = autoChooseDevice();
                if(ac == null) {
                    throw new OCLCheckerException("Autochoose failed to select a GPU");
                }
                platformIndex = ac.getPlatform();
                deviceIndex = ac.getDevice();
            }
            else {
                platformIndex = Nxt.getIntProperty("burst.oclPlatform");
                deviceIndex = Nxt.getIntProperty("burst.oclDevice");
            }

            int[] numPlatforms = new int[1];
            clGetPlatformIDs(0, null, numPlatforms);

            if(numPlatforms[0] == 0) {
                throw new OCLCheckerException("No OpenCL platforms found");
            }

            if(numPlatforms[0] <= platformIndex) {
                throw new OCLCheckerException("Invalid OpenCL platform index");
            }

            cl_platform_id[] platforms = new cl_platform_id[numPlatforms[0]];
            clGetPlatformIDs(platforms.length, platforms, null);

            cl_platform_id platform = platforms[platformIndex];

            int[] numDevices = new int[1];
            clGetDeviceIDs(platform, CL_DEVICE_TYPE_GPU, 0, null, numDevices);

            if(numDevices[0] == 0) {
                throw new OCLCheckerException("No OpenCl Devices found");
            }

            if(numDevices[0] <= deviceIndex) {
                throw new OCLCheckerException("Invalid OpenCL device index");
            }

            cl_device_id[] devices = new cl_device_id[numDevices[0]];
            clGetDeviceIDs(platform, CL_DEVICE_TYPE_GPU, devices.length, devices, null);

            cl_device_id device = devices[deviceIndex];

            if(!checkAvailable(device)) {
                throw new OCLCheckerException("Chosen GPU must be available");
            }

            if(!checkLittleEndian(device)) {
                throw new OCLCheckerException("Chosen GPU must be little endian");
            }

            cl_context_properties ctxProps = new cl_context_properties();
            ctxProps.addProperty(CL_CONTEXT_PLATFORM, platform);

            ctx = clCreateContext(ctxProps, 1, new cl_device_id[]{device}, null, null, null);
            queue = clCreateCommandQueue(ctx, device, 0, null);

            String source;
            try {
                source = new String(Files.readAllBytes(Paths.get("genscoop.cl")));
            } catch (IOException e) {
                throw new OCLCheckerException("Cannot read ocl file", e);
            }

            program = clCreateProgramWithSource(ctx, 1, new String[]{source}, null, null);
            clBuildProgram(program, 0, null, null, null, null);

            genKernel = clCreateKernel(program, "generate_scoops", null);
            getKernel = clCreateKernel(program, "get_scoops", null);

            long[] genGroupSize = new long[1];
            long[] getGroupSize = new long[1];
            clGetKernelWorkGroupInfo(genKernel, device, CL_KERNEL_WORK_GROUP_SIZE, 8, Pointer.to(genGroupSize), null);
            clGetKernelWorkGroupInfo(getKernel, device, CL_KERNEL_WORK_GROUP_SIZE, 8, Pointer.to(getGroupSize), null);

            maxGroupItems = Math.min(genGroupSize[0], getGroupSize[0]);

            if(maxGroupItems <= 0) {
                throw new OCLCheckerException("OpenCL init error. Invalid max group items: " + maxGroupItems);
            }

            long maxItemsByComputeUnits = getComputeUnits(device) * maxGroupItems;

            maxItems = Math.min(calculateMaxItemsByMem(device), maxItemsByComputeUnits);

            if(maxItems % maxGroupItems != 0) {
                maxItems -= (maxItems % maxGroupItems);
            }

            if(maxItems <= 0) {
                throw new OCLCheckerException("OpenCL init error. Invalid calculated max items: " + maxItems);
            }
            Logger.logMessage("OCL max items: " + maxItems);
        }
        catch(CLException e) {
            Logger.logErrorMessage("OpenCL exception: " + e.getMessage());
            e.printStackTrace();
            destroy();
            throw new OCLCheckerException("OpenCL exception", e);
        }
    }

    public static long getMaxItems() { return maxItems; }

    public static void validatePoC(Collection<BlockImpl> blocks) {
        try {
            //System.out.println("starting ocl verify for: " + blocks.size());

            byte[] scoopsOut = new byte[MiningPlot.SCOOP_SIZE * blocks.size()];

            long jobSize = blocks.size();
            if (jobSize % maxGroupItems != 0) {
                jobSize += (maxGroupItems - (jobSize % maxGroupItems));
            }

            if (jobSize > maxItems) {
                throw new IllegalStateException("Attempted to validate too many blocks at once with OCL");
            }
            //System.out.println("ocl blocks: " + blocks.size() + " jobSize: " + jobSize);

            long[] ids = new long[blocks.size()];
            long[] nonces = new long[blocks.size()];
            int[] scoopNums = new int[blocks.size()];

            ByteBuffer buffer = ByteBuffer.allocate(16);
            int i = 0;
            for (BlockImpl block : blocks) {
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.putLong(block.getGeneratorId());
                buffer.putLong(block.getNonce());
                buffer.flip();
                buffer.order(ByteOrder.BIG_ENDIAN);
                ids[i] = buffer.getLong();
                nonces[i] = buffer.getLong();
                buffer.clear();
                scoopNums[i] = block.getScoopNum();
                i++;
            }
            //System.out.println("finished preprocessing: " + blocks.size());

            synchronized (oclLock) {
                if(ctx == null) {
                    throw new OCLCheckerException("OCL context no longer exists");
                }

                cl_mem idMem = null;
                cl_mem nonceMem = null;
                cl_mem bufferMem = null;
                cl_mem scoopNumMem = null;
                cl_mem scoopOutMem = null;

                try {
                    idMem = clCreateBuffer(ctx, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, 8 * blocks.size(), Pointer.to(ids), null);
                    nonceMem = clCreateBuffer(ctx, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, 8 * blocks.size(), Pointer.to(nonces), null);
                    bufferMem = clCreateBuffer(ctx, CL_MEM_READ_WRITE, (MiningPlot.PLOT_SIZE + 16) * blocks.size(), null, null);
                    scoopNumMem = clCreateBuffer(ctx, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, 4 * blocks.size(), Pointer.to(scoopNums), null);
                    scoopOutMem = clCreateBuffer(ctx, CL_MEM_READ_WRITE, MiningPlot.SCOOP_SIZE * blocks.size(), null, null);

                    int[] totalSize = new int[]{blocks.size()};

                    clSetKernelArg(genKernel, 0, Sizeof.cl_mem, Pointer.to(idMem));
                    clSetKernelArg(genKernel, 1, Sizeof.cl_mem, Pointer.to(nonceMem));
                    clSetKernelArg(genKernel, 2, Sizeof.cl_mem, Pointer.to(bufferMem));
                    clSetKernelArg(genKernel, 5, Sizeof.cl_int, Pointer.to(totalSize));

                    int c = 0;
                    int step = hashesPerEnqueue;
                    int[] cur = new int[1];
                    int[] st = new int[1];
                    while (c < 8192) {
                        cur[0] = c;
                        st[0] = (c + step) > 8192 ? 8192 - c : step;
                        clSetKernelArg(genKernel, 3, Sizeof.cl_int, Pointer.to(cur));
                        clSetKernelArg(genKernel, 4, Sizeof.cl_int, Pointer.to(st));
                        clEnqueueNDRangeKernel(queue, genKernel, 1, null, new long[]{jobSize}, new long[]{maxGroupItems}, 0, null, null);

                        c += st[0];
                    }

                    clSetKernelArg(getKernel, 0, Sizeof.cl_mem, Pointer.to(scoopNumMem));
                    clSetKernelArg(getKernel, 1, Sizeof.cl_mem, Pointer.to(bufferMem));
                    clSetKernelArg(getKernel, 2, Sizeof.cl_mem, Pointer.to(scoopOutMem));
                    clSetKernelArg(getKernel, 3, Sizeof.cl_int, Pointer.to(totalSize));

                    clEnqueueNDRangeKernel(queue, getKernel, 1, null, new long[]{jobSize}, new long[]{maxGroupItems}, 0, null, null);

                    clEnqueueReadBuffer(queue, scoopOutMem, true, 0, MiningPlot.SCOOP_SIZE * blocks.size(), Pointer.to(scoopsOut), 0, null, null);
                }
                finally {
                    if(idMem != null) {
                        clReleaseMemObject(idMem);
                    }
                    if(nonceMem != null) {
                        clReleaseMemObject(nonceMem);
                    }
                    if(bufferMem != null) {
                        clReleaseMemObject(bufferMem);
                    }
                    if(scoopNumMem != null) {
                        clReleaseMemObject(scoopNumMem);
                    }
                    if(scoopOutMem != null) {
                        clReleaseMemObject(scoopOutMem);
                    }
                }
            }

            //System.out.println("finished ocl, doing rest: " + blocks.size());

            ByteBuffer scoopsBuffer = ByteBuffer.wrap(scoopsOut);
            byte[] scoop = new byte[MiningPlot.SCOOP_SIZE];

            for (BlockImpl block : blocks) {
                try {
                    scoopsBuffer.get(scoop);
                    block.preVerify(scoop);
                }
                catch (BlockchainProcessor.BlockNotAcceptedException e) {
                    throw new PreValidateFailException("Block failed to prevalidate", e, block);
                }
            }
            //System.out.println("finished rest: " + blocks.size());
        }
        catch(CLException e) {
            throw new OCLCheckerException("OpenCL error", e); // intentionally leave out of unverified cache. It won't slow it that much on one failure and avoids infinite looping on repeat failed attempts.
        }
    }

    static void destroy() {
        synchronized (oclLock) {
            if (program != null) {
                clReleaseProgram(program);
                program = null;
            }
            if (genKernel != null) {
                clReleaseKernel(genKernel);
                genKernel = null;
            }
            if (getKernel != null) {
                clReleaseKernel(getKernel);
                getKernel = null;
            }
            if (queue != null) {
                clReleaseCommandQueue(queue);
                queue = null;
            }
            if (ctx != null) {
                clReleaseContext(ctx);
                ctx = null;
            }
        }
    }

    static private boolean checkAvailable(cl_device_id device) {
        long[] available = new long[1];
        clGetDeviceInfo(device, CL_DEVICE_AVAILABLE, Sizeof.cl_long, Pointer.to(available), null);
        return available[0] == 1;
    }

    static private boolean checkLittleEndian(cl_device_id device) { // idk if the kernel works on big endian, but I'm guessing not and I don't have the hardware to find out
        long[] endianLittle = new long[1];
        clGetDeviceInfo(device, CL_DEVICE_ENDIAN_LITTLE, Sizeof.cl_long, Pointer.to(endianLittle), null);
        return endianLittle[0] == 1;
    }

    static private int getComputeUnits(cl_device_id device) {
        int[] maxComputeUnits = new int[1];
        clGetDeviceInfo(device, CL_DEVICE_MAX_COMPUTE_UNITS, 4, Pointer.to(maxComputeUnits), null);
        return maxComputeUnits[0];
    }

    static private long calculateMaxItemsByMem(cl_device_id device) {
        long[] globalMemSize = new long[1];
        long[] maxMemAllocSize = new long[1];

        clGetDeviceInfo(device, CL_DEVICE_GLOBAL_MEM_SIZE, 8, Pointer.to(globalMemSize), null);
        clGetDeviceInfo(device, CL_DEVICE_MAX_MEM_ALLOC_SIZE, 8, Pointer.to(maxMemAllocSize), null);

        long maxItemsByGlobalMemSize = (globalMemSize[0] * memPercent / 100) / memPerItem;
        long maxItemsByMaxAllocSize = (maxMemAllocSize[0] * memPercent / 100) / bufferPerItem;

        return Math.min(maxItemsByGlobalMemSize, maxItemsByMaxAllocSize);
    }

    static private AutoChooseResult autoChooseDevice() {

        int[] numPlatforms = new int[1];
        clGetPlatformIDs(0, null, numPlatforms);

        if(numPlatforms[0] == 0) {
            throw new OCLCheckerException("No OpenCL platforms found");
        }

        cl_platform_id[] platforms = new cl_platform_id[numPlatforms[0]];
        clGetPlatformIDs(platforms.length, platforms, null);

        AutoChooseResult bestResult = null;
        long bestScore = 0;
        boolean intel = false;
        for(int pfi = 0; pfi < platforms.length; pfi++) {
            long[] platformNameSize = new long[1];
            clGetPlatformInfo(platforms[pfi], CL_PLATFORM_NAME, 0, null, platformNameSize);
            byte[] platformNameChars = new byte[(int)platformNameSize[0]];
            clGetPlatformInfo(platforms[pfi], CL_PLATFORM_NAME, platformNameChars.length, Pointer.to(platformNameChars), null);
            String platformName = new String(platformNameChars);

            System.out.println("Platform " + pfi + ": " + platformName);

            int[] numDevices = new int[1];
            clGetDeviceIDs(platforms[pfi], CL_DEVICE_TYPE_GPU, 0, null, numDevices);

            if(numDevices[0] == 0) {
                continue;
            }

            cl_device_id[] devices = new cl_device_id[numDevices[0]];
            clGetDeviceIDs(platforms[pfi], CL_DEVICE_TYPE_GPU, devices.length, devices, null);

            for(int dvi = 0; dvi < devices.length; dvi++) {
                if(!checkAvailable(devices[dvi])) {
                    continue;
                }

                if(!checkLittleEndian(devices[dvi])) {
                    continue;
                }

                if(bestResult != null && platformName.toLowerCase().contains("intel")) {
                    continue;
                }

                long[] clock = new long[1];
                clGetDeviceInfo(devices[dvi], CL_DEVICE_MAX_CLOCK_FREQUENCY, Sizeof.cl_long, Pointer.to(clock), null);

                long maxItemsAtOnce = Math.min(calculateMaxItemsByMem(devices[dvi]), getComputeUnits(devices[dvi]) * 256);

                long score = maxItemsAtOnce * clock[0];

                if(bestResult == null || score > bestScore || intel) {
                    bestResult = new AutoChooseResult(pfi, dvi);
                    bestScore = score;
                    if(platformName.toLowerCase().contains("intel")) {
                        intel = true;
                    }
                }
            }
        }

        return bestResult;
    }

    static private class AutoChooseResult {
        int platform;
        int device;

        AutoChooseResult(int platform, int device) {
            this.platform = platform;
            this.device = device;
        }

        int getPlatform() {
            return platform;
        }

        int getDevice() {
            return device;
        }
    }

    public static class OCLCheckerException extends RuntimeException {
        OCLCheckerException(String message) {
            super(message);
        }
        OCLCheckerException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class PreValidateFailException extends RuntimeException {
        final BlockImpl block;
        PreValidateFailException(String message, BlockImpl block) {
            super(message);
            this.block = block;
        }
        PreValidateFailException(String message, Throwable cause, BlockImpl block) {
            super(message, cause);
            this.block = block;
        }
        public BlockImpl getBlock() {
            return block;
        }
    }
}
