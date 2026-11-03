public  record ApiError(
    int status,
    String code,
    String path,
    String message){}
