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

    private static cl_context ctx;
    private static cl_command_queue queue;
    private static cl_program program;
    private static cl_kernel genKernel;
    private static cl_kernel getKernel;

    private static long maxItems;
    private static long maxGroupItems;

    private static final Object oclLock = new Object();

    static void init() {}

    static {
        try {
            boolean autoChoose = Nxt.getBooleanProperty("burst.oclAuto");
            setExceptionsEnabled(true);

            int platformIndex;
            int deviceIndex;
            if(autoChoose) {
                AutoChooseResult ac = autoChooseDevice();
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

            long[] endianLittle = new long[1];
            clGetDeviceInfo(device, CL_DEVICE_ENDIAN_LITTLE, Sizeof.cl_long, Pointer.to(endianLittle), null); // idk if the kernel works on big endian, but I'm guessing not and I don't have the hardware to find out
            if(endianLittle[0] != 1) {
                throw new OCLCheckerException("Chosen GPU must be little endian");
            }

            long[] globalMemSize = new long[1];
            long[] maxMemAllocSize = new long[1];
            int[] maxComputeUnits = new int[1];

            clGetDeviceInfo(device, CL_DEVICE_GLOBAL_MEM_SIZE, 8, Pointer.to(globalMemSize), null);
            clGetDeviceInfo(device, CL_DEVICE_MAX_MEM_ALLOC_SIZE, 8, Pointer.to(maxMemAllocSize), null);
            clGetDeviceInfo(device, CL_DEVICE_MAX_COMPUTE_UNITS, 4, Pointer.to(maxComputeUnits), null);

            long memPerItem = 8 // id
                    + 8 // nonce
                    + MiningPlot.PLOT_SIZE + 16 // buffer
                    + 4 // scoop num
                    + MiningPlot.SCOOP_SIZE; // output scoop

            long bufferPerItem = MiningPlot.PLOT_SIZE + 16;

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

            int memPercent = Nxt.getIntProperty("burst.oclMemPercent");
            if(memPercent == 0) {
                Logger.logMessage("OpenCL mem percent not specified, using default: " + DEFAULT_MEM_PERCENT);
                memPercent = DEFAULT_MEM_PERCENT;
            }

            long maxItemsByGlobalMemSize = (globalMemSize[0] * memPercent / 100) / memPerItem;
            long maxItemsByMaxAllocSize = (maxMemAllocSize[0] * memPercent / 100) / bufferPerItem;
            long maxItemsByComputeUnits = maxComputeUnits[0] * maxGroupItems;

            maxItems = Math.min(Math.min(maxItemsByGlobalMemSize, maxItemsByMaxAllocSize), maxItemsByComputeUnits);

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

    public static void validatePoC(Collection<BlockImpl> blocks) throws BlockchainProcessor.BlockNotAcceptedException {

        System.out.println("starting ocl verify for: " + blocks.size());

        byte[] scoopsOut = new byte[MiningPlot.SCOOP_SIZE * blocks.size()];

        long jobSize = blocks.size();
        if (jobSize % maxGroupItems != 0) {
            jobSize += (maxGroupItems - (jobSize % maxGroupItems));
        }

        if (jobSize > maxItems) {
            throw new IllegalStateException("Attempted to validate too many blocks at once with OCL");
        }
        System.out.println("ocl blocks: " + blocks.size() + " jobSize: " + jobSize);

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
        System.out.println("finished preprocessing: " + blocks.size());

        synchronized (oclLock) {
            cl_mem idMem = clCreateBuffer(ctx, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, 8 * blocks.size(), Pointer.to(ids), null);
            cl_mem nonceMem = clCreateBuffer(ctx, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, 8 * blocks.size(), Pointer.to(nonces), null);
            cl_mem bufferMem = clCreateBuffer(ctx, CL_MEM_READ_WRITE, (MiningPlot.PLOT_SIZE + 16) * blocks.size(), null, null);
            cl_mem scoopNumMem = clCreateBuffer(ctx, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, 4 * blocks.size(), Pointer.to(scoopNums), null);
            cl_mem scoopOutMem = clCreateBuffer(ctx, CL_MEM_READ_WRITE, MiningPlot.SCOOP_SIZE * blocks.size(), null, null);

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

            clReleaseMemObject(idMem);
            clReleaseMemObject(nonceMem);
            clReleaseMemObject(bufferMem);
            clReleaseMemObject(scoopNumMem);
            clReleaseMemObject(scoopOutMem);
        }

        System.out.println("finished ocl, doing rest: " + blocks.size());

        ByteBuffer scoopsBuffer = ByteBuffer.wrap(scoopsOut);
        byte[] scoop = new byte[MiningPlot.SCOOP_SIZE];

        for(BlockImpl block : blocks) {
            scoopsBuffer.get(scoop);
            block.preVerify(scoop);
        }
        System.out.println("finished rest: " + blocks.size());
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

    static private AutoChooseResult autoChooseDevice() {

        return new AutoChooseResult(0, 0);
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
}
