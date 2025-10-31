
export type Task = {
    id: string;
    payload: string;
    type: string;
    status: string;
    attempts: number;
    maxRetries: number;
    createdAt: string;
}
