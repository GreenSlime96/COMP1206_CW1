import static org.jocl.CL.*;

import org.jocl.*;

/**
 * A small JOCL sample.
 */
public class JOCLAlgorithm
{
    /**
     * The source code of the OpenCL program to execute
     */
    private static String programSource =
        "__kernel void "+
        "sampleKernel(__global const double *x,"+
        "             __global const double *y,"+
        "             __global const int *w,"+
        "             __global const double *s,"+   
        "             __global const double *r,"+ 
        "             __global const int *i,"+ 
        "             __global double *o)"+
        "{"+
        "    int gid = get_global_id(0);"+
        "  "+
        "    int x0 = gid % w[0];"+
        "    int y0 = gid / w[0];"+
        " "+
        "    double cx = x[0] + x0 * s[0];"+
        "    double cy = y[0] - y0 * s[0];"+
        " "+
        "    double zx = cx;"+
        "    double zy = cy;"+      
        " "+
        "    double r2 = r[0] * r[0];"+
        " "+
        "    double iterations = 0;"+
        "    while (zx * zx + zy * zy < r2 && iterations < i[0]) {"+
        "        double xt = zx * zx - zy * zy + cx;"+
        "        double yt = 2 * zx * zy + cy;" +
        " "+
        "        if (zx == xt && zx == yt) {"+
        "            iterations = i[0];"+
        "            break;"+
        "        }"+
        " "+
        "        zx = xt;"+
        "        zy = yt;"+
        " "+
        "        iterations ++;"+
        "    }"+
        " "+
        "    if (iterations < i[0]) {"+
        "        double zn_abs = sqrt(zx * zx + zy * zy);"+
        "        double u = log(log(zn_abs) / log(r2)) / log(2.0);"+
        "        iterations += 1 - u;"+
        "    }"+
        " "+
        "    o[gid] = min(iterations, (double) i[0]);"+
        "}";
    

    /**
     * The entry point of this sample
     * 
     * @param args Not used
     */
    public static double[] getArray(double x, double y, int w, double s, double r, int i)
    {
        // Create input- and output data 
        int n = 917560;
        double dstArray[] = new double[n];

        Pointer srcA = Pointer.to(new double[] {x});
        Pointer srcB = Pointer.to(new double[] {y});
        Pointer srcC = Pointer.to(new int[] {w});
        Pointer srcD = Pointer.to(new double[] {s});
        Pointer srcE = Pointer.to(new double[] {r});
        Pointer srcF = Pointer.to(new int[] {i});
        Pointer dst = Pointer.to(dstArray);

        // The platform, device type and device number
        // that will be used
        final int platformIndex = 0;
        final long deviceType = CL_DEVICE_TYPE_ALL;
        final int deviceIndex = 0;

        // Enable exceptions and subsequently omit error checks in this sample
        CL.setExceptionsEnabled(true);

        // Obtain the number of platforms
        int numPlatformsArray[] = new int[1];
        clGetPlatformIDs(0, null, numPlatformsArray);
        int numPlatforms = numPlatformsArray[0];

        // Obtain a platform ID
        cl_platform_id platforms[] = new cl_platform_id[numPlatforms];
        clGetPlatformIDs(platforms.length, platforms, null);
        cl_platform_id platform = platforms[platformIndex];

        // Initialize the context properties
        cl_context_properties contextProperties = new cl_context_properties();
        contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform);
        
        // Obtain the number of devices for the platform
        int numDevicesArray[] = new int[1];
        clGetDeviceIDs(platform, deviceType, 0, null, numDevicesArray);
        int numDevices = numDevicesArray[0];
        
        // Obtain a device ID 
        cl_device_id devices[] = new cl_device_id[numDevices];
        clGetDeviceIDs(platform, deviceType, numDevices, devices, null);
        cl_device_id device = devices[deviceIndex];

        // Create a context for the selected device
        cl_context context = clCreateContext(
            contextProperties, 1, new cl_device_id[]{device}, 
            null, null, null);
        
        // Create a command-queue for the selected device
        cl_command_queue commandQueue = 
            clCreateCommandQueue(context, device, 0, null);

        // Allocate the memory objects for the input- and output data
		cl_mem memObjects[] = new cl_mem[7];
		memObjects[0] = clCreateBuffer(context, CL_MEM_READ_ONLY
				| CL_MEM_COPY_HOST_PTR, Sizeof.cl_double, srcA, null);
		memObjects[1] = clCreateBuffer(context, CL_MEM_READ_ONLY
				| CL_MEM_COPY_HOST_PTR, Sizeof.cl_double, srcB, null);
		memObjects[2] = clCreateBuffer(context, CL_MEM_READ_ONLY
				| CL_MEM_COPY_HOST_PTR, Sizeof.cl_int, srcC, null);
		memObjects[3] = clCreateBuffer(context, CL_MEM_READ_ONLY
				| CL_MEM_COPY_HOST_PTR, Sizeof.cl_double, srcD, null);
		memObjects[4] = clCreateBuffer(context, CL_MEM_READ_ONLY
				| CL_MEM_COPY_HOST_PTR, Sizeof.cl_double, srcE, null);
		memObjects[5] = clCreateBuffer(context, CL_MEM_READ_ONLY
				| CL_MEM_COPY_HOST_PTR, Sizeof.cl_int, srcF, null);
		memObjects[6] = clCreateBuffer(context, CL_MEM_READ_WRITE,
				Sizeof.cl_double * n, null, null);
        
        // Create the program from the source code
        cl_program program = clCreateProgramWithSource(context,
            1, new String[]{ programSource }, null, null);
        
        // Build the program
        clBuildProgram(program, 0, null, null, null, null);
        
        // Create the kernel
        cl_kernel kernel = clCreateKernel(program, "sampleKernel", null);
        
        // Set the arguments for the kernel
        clSetKernelArg(kernel, 0, 
            Sizeof.cl_mem, Pointer.to(memObjects[0]));
        clSetKernelArg(kernel, 1, 
            Sizeof.cl_mem, Pointer.to(memObjects[1]));
        clSetKernelArg(kernel, 2, 
            Sizeof.cl_mem, Pointer.to(memObjects[2]));
        clSetKernelArg(kernel, 3, 
            Sizeof.cl_mem, Pointer.to(memObjects[3]));
        clSetKernelArg(kernel, 4, 
            Sizeof.cl_mem, Pointer.to(memObjects[4]));
        clSetKernelArg(kernel, 5, 
            Sizeof.cl_mem, Pointer.to(memObjects[5]));
        clSetKernelArg(kernel, 6, 
                Sizeof.cl_mem, Pointer.to(memObjects[6]));
        
        // Set the work-item dimensions
        long global_work_size[] = new long[]{n};
        long local_work_size[] = new long[]{1};
        
        // Execute the kernel
        clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
            global_work_size, local_work_size, 0, null, null);
        
        // Read the output data
        clEnqueueReadBuffer(commandQueue, memObjects[6], CL_TRUE, 0,
            n * Sizeof.cl_double, dst, 0, null, null);
        
        // Release kernel, program, and memory objects
        clReleaseMemObject(memObjects[0]);
        clReleaseMemObject(memObjects[1]);
        clReleaseMemObject(memObjects[2]);
        clReleaseMemObject(memObjects[3]);
        clReleaseMemObject(memObjects[4]);
        clReleaseMemObject(memObjects[5]);
        clReleaseKernel(kernel);
        clReleaseProgram(program);
        clReleaseCommandQueue(commandQueue);
        clReleaseContext(context);
        
        // Verify the result
        return dstArray;
    }
}