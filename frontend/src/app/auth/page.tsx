'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import api from '@/lib/api';
import { useAuthStore } from '@/store/authStore';

export default function AuthPage() {
  const router = useRouter();
  const login = useAuthStore((s) => s.login);
  const [isLogin, setIsLogin] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const [form, setForm] = useState({
    email: '',
    password: '',
    passwordConfirm: '',
    name: '',
    studentId: '',
    role: 'STUDENT',
  });

  const onChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }));

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      if (!isLogin && form.password !== form.passwordConfirm) {
        setError('비밀번호가 일치하지 않습니다.');
        setLoading(false);
        return;
      }

      const endpoint = isLogin ? '/auth/login' : '/auth/signup';
      const payload = isLogin
        ? { email: form.email, password: form.password }
        : { email: form.email, password: form.password, name: form.name, studentId: form.studentId, role: form.role };

      const res = await api.post(endpoint, payload);
      const { accessToken, refreshToken, user } = res.data;
      login(accessToken, refreshToken, user);
      router.replace('/dashboard');
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || '오류가 발생했습니다.';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900">
      <div className="w-full max-w-md">
        {/* Logo */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center gap-2 mb-2">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-violet-500 to-indigo-600 flex items-center justify-center text-white font-black text-lg">B</div>
            <span className="text-2xl font-bold text-white tracking-tight">Blackbox</span>
          </div>
          <p className="text-slate-400 text-sm">팀 프로젝트 기여도 분석 플랫폼</p>
        </div>

        <div className="bg-slate-800/60 backdrop-blur border border-slate-700/50 rounded-2xl p-8 shadow-2xl">
          {/* Tab */}
          <div className="flex rounded-xl bg-slate-900/50 p-1 mb-6">
            <button
              type="button"
              className={`flex-1 py-2 rounded-lg text-sm font-medium transition-all ${isLogin ? 'bg-violet-600 text-white shadow' : 'text-slate-400 hover:text-white'}`}
              onClick={() => { setIsLogin(true); setError(''); }}
            >
              로그인
            </button>
            <button
              type="button"
              className={`flex-1 py-2 rounded-lg text-sm font-medium transition-all ${!isLogin ? 'bg-violet-600 text-white shadow' : 'text-slate-400 hover:text-white'}`}
              onClick={() => { setIsLogin(false); setError(''); }}
            >
              회원가입
            </button>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            {!isLogin && (
              <>
                <InputField id="name" name="name" label="이름" type="text" value={form.name} onChange={onChange} placeholder="홍길동" required />
                <InputField id="studentId" name="studentId" label="학번 (선택)" type="text" value={form.studentId} onChange={onChange} placeholder="20241234" />
                <div>
                  <label htmlFor="role" className="block text-sm font-medium text-slate-300 mb-1.5">역할</label>
                  <select
                    id="role"
                    name="role"
                    value={form.role}
                    onChange={onChange}
                    className="w-full px-4 py-2.5 rounded-xl bg-slate-900/60 border border-slate-600 text-white focus:outline-none focus:ring-2 focus:ring-violet-500 text-sm"
                  >
                    <option value="STUDENT">학생</option>
                    <option value="PROFESSOR">교수</option>
                    <option value="TA">조교</option>
                  </select>
                </div>
              </>
            )}

            <InputField id="email" name="email" label="이메일" type="email" value={form.email} onChange={onChange} placeholder="you@university.ac.kr" required />
            <InputField id="password" name="password" label="비밀번호" type="password" value={form.password} onChange={onChange} placeholder={isLogin ? '비밀번호' : '8자 이상'} required />
            {!isLogin && (
              <InputField id="passwordConfirm" name="passwordConfirm" label="비밀번호 확인" type="password" value={form.passwordConfirm} onChange={onChange} placeholder="비밀번호 재입력" required />
            )}

            {error && (
              <div className="bg-red-500/10 border border-red-500/30 rounded-lg px-4 py-2.5 text-red-400 text-sm">
                {error}
              </div>
            )}

            <button
              type="submit"
              disabled={loading}
              className="w-full py-3 rounded-xl bg-gradient-to-r from-violet-600 to-indigo-600 hover:from-violet-500 hover:to-indigo-500 text-white font-semibold text-sm transition-all shadow-lg shadow-violet-900/30 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? '처리 중...' : isLogin ? '로그인' : '회원가입'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}

function InputField({
  id, name, label, type, value, onChange, placeholder, required,
}: {
  id: string; name: string; label: string; type: string;
  value: string; onChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  placeholder?: string; required?: boolean;
}) {
  return (
    <div>
      <label htmlFor={id} className="block text-sm font-medium text-slate-300 mb-1.5">{label}</label>
      <input
        id={id}
        name={name}
        type={type}
        value={value}
        onChange={onChange}
        placeholder={placeholder}
        required={required}
        className="w-full px-4 py-2.5 rounded-xl bg-slate-900/60 border border-slate-600 text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-violet-500 text-sm transition-all"
      />
    </div>
  );
}
