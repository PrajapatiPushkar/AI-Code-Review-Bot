import React, { useState, useEffect, useRef } from 'react';
import { useParams } from 'react-router-dom';
import reviewService from '../services/reviewService';
import ErrorMessage from '../components/ErrorMessage';
import ReviewHeader from '../components/review-detail/ReviewHeader';
import ReviewMetrics from '../components/review-detail/ReviewMetrics';
import ReviewMetadata from '../components/review-detail/ReviewMetadata';
import ReviewSummary from '../components/review-detail/ReviewSummary';
import ReviewActions from '../components/review-detail/ReviewActions';
import ReviewDetailsSkeleton from '../components/review-detail/ReviewDetailsSkeleton';

const ReviewDetailsPage = () => {
  const { id } = useParams();

  const [review, setReview] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isPolling, setIsPolling] = useState(false);
  const [pollCount, setPollCount] = useState(0);

  const pollTimerRef = useRef(null);

  // Initial fetch
  const fetchReviewDetails = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await reviewService.getReviewById(id);
      setReview(data);
      if (data.status === 'IN_PROGRESS') {
        setIsPolling(true);
      }
    } catch (err) {
      setError(err.response?.data?.message || `Failed to load review details for ID #${id}`);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchReviewDetails();

    return () => {
      if (pollTimerRef.current) {
        clearInterval(pollTimerRef.current);
      }
    };
  }, [id]);

  // Polling logic when status is IN_PROGRESS
  useEffect(() => {
    if (!isPolling) return;

    pollTimerRef.current = setInterval(async () => {
      try {
        setPollCount((prev) => prev + 1);
        const statusData = await reviewService.getReviewStatus(id);

        if (statusData && statusData.status !== 'IN_PROGRESS') {
          // Polling reached terminal state (COMPLETED or FAILED)
          clearInterval(pollTimerRef.current);
          setIsPolling(false);

          // Fetch complete result if completed
          if (statusData.status === 'COMPLETED') {
            const resultData = await reviewService.getReviewResult(id);
            setReview((prev) => ({
              ...prev,
              ...resultData,
              id: prev?.id || resultData?.codeReviewId || id,
              status: 'COMPLETED'
            }));
          } else {
            // FAILED
            setReview((prev) => ({
              ...prev,
              ...statusData,
              id: prev?.id || statusData?.codeReviewId || id,
              status: statusData.status || 'FAILED'
            }));
          }
        }
      } catch (err) {
        // Stop polling on repeated errors
        clearInterval(pollTimerRef.current);
        setIsPolling(false);
      }
    }, 2500);

    // Timeout safety after 60 seconds (24 polling attempts)
    const timeoutTimer = setTimeout(() => {
      if (pollTimerRef.current) {
        clearInterval(pollTimerRef.current);
        setIsPolling(false);
      }
    }, 60000);

    return () => {
      if (pollTimerRef.current) clearInterval(pollTimerRef.current);
      clearTimeout(timeoutTimer);
    };
  }, [id, isPolling]);

  const formatDuration = (createdStr, completedStr) => {
    if (!createdStr || !completedStr) return 'N/A';
    const created = new Date(createdStr);
    const completed = new Date(completedStr);
    const diffMs = completed - created;
    if (diffMs <= 0) return '< 1s';
    const seconds = Math.floor(diffMs / 1000);
    if (seconds < 60) return `${seconds}s`;
    const minutes = Math.floor(seconds / 60);
    return `${minutes}m ${seconds % 60}s`;
  };

  if (loading) {
    return <ReviewDetailsSkeleton />;
  }

  if (error) {
    return (
      <div className="review-detail-page">
        <ErrorMessage message={error} onRetry={fetchReviewDetails} />
      </div>
    );
  }

  if (!review) {
    return (
      <div className="review-detail-page">
        <ErrorMessage message="Code review not found." onRetry={fetchReviewDetails} />
      </div>
    );
  }

  const isCompleted = review.status === 'COMPLETED';
  const isFailed = review.status === 'FAILED';
  const isInProgress = review.status === 'IN_PROGRESS';

  const repoName = review.repository || review.repositoryName;
  const githubPrUrl =
    review.owner && repoName && review.pullRequestNumber
      ? `https://github.com/${review.owner}/${repoName}/pull/${review.pullRequestNumber}`
      : null;

  const summary = review.summary || review.reviewSummary || '';

  return (
    <div className="review-detail-page">
      {/* Page Header with Context & Direct Actions */}
      <ReviewHeader
        review={review}
        githubPrUrl={githubPrUrl}
        isCompleted={isCompleted}
        isInProgress={isInProgress}
        isFailed={isFailed}
      />

      {/* State Banner (In-Progress, Failed, or Completed bar) */}
      <ReviewActions
        review={review}
        isInProgress={isInProgress}
        isFailed={isFailed}
        isCompleted={isCompleted}
        pollCount={pollCount}
        githubPrUrl={githubPrUrl}
      />

      {/* Top Metrics Row */}
      <ReviewMetrics
        review={review}
        formatDuration={formatDuration}
      />

      {/* Main Two-Column Content Grid: Metadata & AI Summary */}
      <div className="review-detail-content-grid">
        {/* Left Column: Metadata Overview */}
        <div className="review-detail-grid-column">
          <ReviewMetadata
            review={review}
            formatDuration={formatDuration}
            githubPrUrl={githubPrUrl}
          />
        </div>

        {/* Right Column: AI Review Summary */}
        <div className="review-detail-grid-column">
          <ReviewSummary
            summary={summary}
            isInProgress={isInProgress}
          />
        </div>
      </div>
    </div>
  );
};

export default ReviewDetailsPage;
