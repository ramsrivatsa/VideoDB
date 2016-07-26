//
// MATLAB Compiler: 5.1 (R2014a)
// Date: Tue Jul 26 13:30:17 2016
// Arguments: "-B" "macro_default" "-v" "-W" "cpplib:libmdnet" "-T" "link:lib"
// "-I" "../utils/" "-I" "../matconvnet/matlab/" "mdnet_run" 
//

#include <stdio.h>
#define EXPORTING_libmdnet 1
#include "libmdnet.h"

static HMCRINSTANCE _mcr_inst = NULL;


#ifdef __cplusplus
extern "C" {
#endif

static int mclDefaultPrintHandler(const char *s)
{
  return mclWrite(1 /* stdout */, s, sizeof(char)*strlen(s));
}

#ifdef __cplusplus
} /* End extern "C" block */
#endif

#ifdef __cplusplus
extern "C" {
#endif

static int mclDefaultErrorHandler(const char *s)
{
  int written = 0;
  size_t len = 0;
  len = strlen(s);
  written = mclWrite(2 /* stderr */, s, sizeof(char)*len);
  if (len > 0 && s[ len-1 ] != '\n')
    written += mclWrite(2 /* stderr */, "\n", sizeof(char));
  return written;
}

#ifdef __cplusplus
} /* End extern "C" block */
#endif

/* This symbol is defined in shared libraries. Define it here
 * (to nothing) in case this isn't a shared library. 
 */
#ifndef LIB_libmdnet_C_API
#define LIB_libmdnet_C_API /* No special import/export declaration */
#endif

LIB_libmdnet_C_API 
bool MW_CALL_CONV libmdnetInitializeWithHandlers(
    mclOutputHandlerFcn error_handler,
    mclOutputHandlerFcn print_handler)
{
    int bResult = 0;
  if (_mcr_inst != NULL)
    return true;
  if (!mclmcrInitialize())
    return false;
    {
        mclCtfStream ctfStream = 
            mclGetEmbeddedCtfStream((void *)(libmdnetInitializeWithHandlers));
        if (ctfStream) {
            bResult = mclInitializeComponentInstanceEmbedded(   &_mcr_inst,
                                                                error_handler, 
                                                                print_handler,
                                                                ctfStream);
            mclDestroyStream(ctfStream);
        } else {
            bResult = 0;
        }
    }  
    if (!bResult)
    return false;
  return true;
}

LIB_libmdnet_C_API 
bool MW_CALL_CONV libmdnetInitialize(void)
{
  return libmdnetInitializeWithHandlers(mclDefaultErrorHandler, mclDefaultPrintHandler);
}

LIB_libmdnet_C_API 
void MW_CALL_CONV libmdnetTerminate(void)
{
  if (_mcr_inst != NULL)
    mclTerminateInstance(&_mcr_inst);
}

LIB_libmdnet_C_API 
void MW_CALL_CONV libmdnetPrintStackTrace(void) 
{
  char** stackTrace;
  int stackDepth = mclGetStackTrace(&stackTrace);
  int i;
  for(i=0; i<stackDepth; i++)
  {
    mclWrite(2 /* stderr */, stackTrace[i], sizeof(char)*strlen(stackTrace[i]));
    mclWrite(2 /* stderr */, "\n", sizeof(char)*strlen("\n"));
  }
  mclFreeStackTrace(&stackTrace, stackDepth);
}


LIB_libmdnet_C_API 
bool MW_CALL_CONV mlxMdnet_run(int nlhs, mxArray *plhs[], int nrhs, mxArray *prhs[])
{
  return mclFeval(_mcr_inst, "mdnet_run", nlhs, plhs, nrhs, prhs);
}

LIB_libmdnet_CPP_API 
void MW_CALL_CONV mdnet_run(int nargout, mwArray& result, const mwArray& images, const 
                            mwArray& region, const mwArray& net)
{
  mclcppMlfFeval(_mcr_inst, "mdnet_run", nargout, 1, 3, &result, &images, &region, &net);
}

