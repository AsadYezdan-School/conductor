import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { api } from '../api/client';
import type { JobDetail } from '../api/types';
import { CronHelper } from './CronHelper';

const isJsonOrEmpty = (val: string) => {
  if (!val.trim()) return true;
  try { JSON.parse(val); return true; } catch { return false; }
};

const schema = z.object({
  name: z.string().min(1, 'Name is required'),
  cron: z.string().min(1, 'Cron is required'),
  url: z.string().url('Must be a valid URL'),
  method: z.string().min(1, 'Method is required'),
  timeoutSeconds: z.coerce.number().int().positive('Must be positive'),
  payloadText: z.string().refine(isJsonOrEmpty, 'Must be valid JSON'),
  headersText: z.string().refine(isJsonOrEmpty, 'Must be valid JSON'),
});

type FormValues = z.infer<typeof schema>;

interface Props {
  existing?: JobDetail;
  onClose: () => void;
}

const HTTP_METHODS = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE'];

export function JobForm({ existing, onClose }: Props) {
  const queryClient = useQueryClient();

  const {
    register,
    handleSubmit,
    watch,
    reset,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: '',
      cron: '',
      url: '',
      method: 'POST',
      timeoutSeconds: 30,
      payloadText: '',
      headersText: '',
    },
  });

  useEffect(() => {
    if (existing) {
      reset({
        name: existing.name,
        cron: existing.cron,
        url: existing.url,
        method: existing.method,
        timeoutSeconds: existing.timeoutSeconds,
        payloadText: existing.payload ? JSON.stringify(JSON.parse(existing.payload), null, 2) : '',
        headersText: existing.headers ? JSON.stringify(JSON.parse(existing.headers), null, 2) : '',
      });
    }
  }, [existing, reset]);

  const createMutation = useMutation({
    mutationFn: (values: FormValues) =>
      api.createJob({
        name: values.name,
        cron: values.cron,
        url: values.url,
        method: values.method,
        timeoutSeconds: values.timeoutSeconds,
        payload: values.payloadText.trim() ? JSON.parse(values.payloadText) : null,
        headers: values.headersText.trim() ? JSON.parse(values.headersText) : null,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['jobs'] });
      toast.success('Job created');
      onClose();
    },
    onError: (e: Error) => toast.error(e.message),
  });

  const editMutation = useMutation({
    mutationFn: (values: FormValues) =>
      api.editJob(existing!.jobFamilyId, {
        name: values.name,
        cron: values.cron,
        url: values.url,
        method: values.method,
        timeoutSeconds: values.timeoutSeconds,
        payload: values.payloadText.trim() ? JSON.parse(values.payloadText) : null,
        headers: values.headersText.trim() ? JSON.parse(values.headersText) : null,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['jobs'] });
      queryClient.invalidateQueries({ queryKey: ['job', existing!.jobFamilyId] });
      toast.success('Job updated');
      onClose();
    },
    onError: (e: Error) => toast.error(e.message),
  });

  const isPending = createMutation.isPending || editMutation.isPending;
  const cronValue = watch('cron');

  function onSubmit(values: FormValues) {
    if (existing) {
      editMutation.mutate(values);
    } else {
      createMutation.mutate(values);
    }
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      <div>
        <label className="block text-sm font-medium text-gray-700">Name</label>
        <input
          {...register('name')}
          className="mt-1 block w-full rounded border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500"
        />
        {errors.name && <p className="mt-1 text-xs text-red-600">{errors.name.message}</p>}
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700">
          Cron
          <CronHelper cron={cronValue} />
        </label>
        <input
          {...register('cron')}
          placeholder="0 * * * *"
          className="mt-1 block w-full rounded border border-gray-300 px-3 py-2 text-sm font-mono focus:outline-none focus:ring-1 focus:ring-blue-500"
        />
        {errors.cron && <p className="mt-1 text-xs text-red-600">{errors.cron.message}</p>}
      </div>

      <div className="flex gap-3">
        <div className="flex-1">
          <label className="block text-sm font-medium text-gray-700">URL</label>
          <input
            {...register('url')}
            placeholder="https://example.com/webhook"
            className="mt-1 block w-full rounded border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
          {errors.url && <p className="mt-1 text-xs text-red-600">{errors.url.message}</p>}
        </div>
        <div className="w-32">
          <label className="block text-sm font-medium text-gray-700">Method</label>
          <select
            {...register('method')}
            className="mt-1 block w-full rounded border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500"
          >
            {HTTP_METHODS.map((m) => (
              <option key={m} value={m}>{m}</option>
            ))}
          </select>
        </div>
      </div>

      <div className="w-40">
        <label className="block text-sm font-medium text-gray-700">Timeout (seconds)</label>
        <input
          type="number"
          {...register('timeoutSeconds')}
          className="mt-1 block w-full rounded border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500"
        />
        {errors.timeoutSeconds && (
          <p className="mt-1 text-xs text-red-600">{errors.timeoutSeconds.message}</p>
        )}
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700">Payload (JSON)</label>
        <textarea
          {...register('payloadText')}
          rows={4}
          className="mt-1 block w-full rounded border border-gray-300 px-3 py-2 text-sm font-mono focus:outline-none focus:ring-1 focus:ring-blue-500"
        />
        {errors.payloadText && (
          <p className="mt-1 text-xs text-red-600">{errors.payloadText.message}</p>
        )}
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700">Headers (JSON)</label>
        <textarea
          {...register('headersText')}
          rows={3}
          placeholder='{"Authorization": "Bearer token"}'
          className="mt-1 block w-full rounded border border-gray-300 px-3 py-2 text-sm font-mono focus:outline-none focus:ring-1 focus:ring-blue-500"
        />
        {errors.headersText && (
          <p className="mt-1 text-xs text-red-600">{errors.headersText.message}</p>
        )}
      </div>

      <div className="flex justify-end gap-3 pt-2">
        <button
          type="button"
          onClick={onClose}
          className="rounded border border-gray-300 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
        >
          Cancel
        </button>
        <button
          type="submit"
          disabled={isPending}
          className="rounded bg-blue-600 px-4 py-2 text-sm text-white hover:bg-blue-700 disabled:opacity-50"
        >
          {isPending ? 'Saving…' : existing ? 'Save changes' : 'Create job'}
        </button>
      </div>
    </form>
  );
}
