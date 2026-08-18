import CvReviewHeader from './CvReviewHeader';
import CvReviewFooter from './CvReviewFooter';

export default function CvReviewLayout({ children }) {
  return (
    <div className="min-h-screen flex flex-col justify-between p-8 md:p-12 w-full">
      {/* Header menempel di baris paling atas */}
      <CvReviewHeader />

      {/* Area konten di tengah */}
      <main className="flex-1 flex items-center justify-center my-6">
        <div className="w-full max-w-6xl">
          {children}
        </div>
      </main>

      {/* Footer menempel di baris paling bawah */}
      <CvReviewFooter />
    </div>
  );
}
