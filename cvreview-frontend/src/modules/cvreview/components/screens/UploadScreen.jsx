import StepIndicator from '../parts/StepIndicator';
import HeroHeading from '../parts/HeroHeading';
import InspirationQuote from '../parts/InspirationQuote';
import HandoffCard from '../parts/HandoffCard';

export default function UploadScreen({ onSubmit, isLoading }) {
  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 lg:gap-16 items-start">
      {/* Left column: messaging */}
      <div className="pt-4">
        <StepIndicator step="01" label="RECEIVE" />
        <HeroHeading
          title="Put the source material here."
          subtitle="A clear first stop for the things that need to move forward. Send a file or leave a note — we'll take it from here."
        />
        <InspirationQuote />
      </div>

      {/* Right column: input card */}
      <div className="pt-4">
        <HandoffCard onSubmit={onSubmit} isLoading={isLoading} />
      </div>
    </div>
  );
}
