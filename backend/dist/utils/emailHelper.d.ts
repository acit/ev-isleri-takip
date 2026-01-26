export declare const checkEmailConfiguration: () => {
    isConfigured: boolean;
    user: string | undefined;
    service: string;
    from: string | undefined;
};
export declare const getEmailStatus: () => {
    status: string;
    message: string;
    mode: string;
};
export declare const formatEmailList: (emails: string[]) => string[];
//# sourceMappingURL=emailHelper.d.ts.map