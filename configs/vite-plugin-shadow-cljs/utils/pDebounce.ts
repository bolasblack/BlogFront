export function pDebounce<TArgs extends unknown[], TResult>(
  fn: (calls: TArgs[]) => TResult | Promise<TResult>,
  delay: number
): (...args: TArgs) => Promise<TResult> {
  let timer: ReturnType<typeof setTimeout> | null = null;
  let pendingCalls: TArgs[] = [];
  let pendingPromises: {
    resolve: (value: TResult) => void;
    reject: (err: unknown) => void;
  }[] = [];

  return (...args: TArgs): Promise<TResult> => {
    pendingCalls.push(args);

    return new Promise((resolve, reject) => {
      pendingPromises.push({ resolve, reject });

      if (timer) {
        clearTimeout(timer);
      }

      timer = setTimeout(async () => {
        const calls = pendingCalls;
        const promises = pendingPromises;
        pendingCalls = [];
        pendingPromises = [];
        timer = null;

        try {
          const result = await fn(calls);
          promises.forEach((p) => p.resolve(result));
        } catch (err) {
          promises.forEach((p) => p.reject(err));
        }
      }, delay);
    });
  };
}
